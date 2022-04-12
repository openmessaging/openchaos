/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openchaos.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openchaos.DriverConfiguration;
import io.openchaos.client.KVClient;
import io.openchaos.client.Client;
import io.openchaos.driver.ChaosNode;
import io.openchaos.worker.ClientWorker;
import io.openchaos.worker.Worker;
import io.openchaos.driver.kv.KVDriver;
import io.openchaos.recorder.Recorder;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KVModel implements Model {
    public static final String MODEL_NAME = "kv";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(KVModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private final List<Client> clients;
    private Optional<String> key;
    private List<ClientWorker> workers;
    private Map<String, ChaosNode> cluster;
    private RateLimiter rateLimiter;
    private Map<String, ChaosNode> metaNodesMap;
    private int concurrency;
    private Recorder recorder;
    private KVDriver driver;
    private File driverConfigFile;
    private boolean restart;

    public KVModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile) {
        this.concurrency = concurrency;
        this.clients = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.cluster = new HashMap<>();
        this.rateLimiter = rateLimiter;
        this.recorder = recorder;
        this.metaNodesMap = new HashMap<>();
        this.driverConfigFile = driverConfigFile;
        this.key = Optional.ofNullable(String.format("%s-chaos-key", DATE_FORMAT.format(new Date())));
    }

    private static KVDriver createKVDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        KVDriver driver;

        try {
            driver = (KVDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            driver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            log.error(String.format("Failed to create %s !", driverConfiguration.driverClass), e);
            throw new RuntimeException(e);
        }

        return driver;
    }

    @Override public void setupClient() {
        try {
            if (driver == null) {
                driver = createKVDriver(driverConfigFile);
            }

            log.info("KV client setup...");

            for (int i = 0; i < concurrency; i++) {
                Client client = new KVClient(driver, recorder, key);
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("kvClient-" + i, client, rateLimiter, log);
                workers.add(clientWorker);
            }

            log.info("{} KV client setup success!", concurrency);

        } catch (Exception e) {
            log.error("KV client failed to start.", e);
            throw new RuntimeException(e);
        }
    }

    @Override public Map<String, ChaosNode> setupCluster(DriverConfiguration driverConfiguration, boolean isInstall, boolean restart) {
        try {
            if (driver == null) {
                driver = createKVDriver(driverConfigFile);
            }

            if (driverConfiguration.metaNodes != null) {
                driverConfiguration.metaNodes.forEach(node -> metaNodesMap.put(node, driver.createChaosMetaNode(node, driverConfiguration.metaNodes)));
            }

            if (driverConfiguration.nodes != null) {
                driverConfiguration.nodes.forEach(node -> cluster.put(node, driver.createChaosNode(node, driverConfiguration.nodes)));
            }

            if (isInstall) {
                metaNodesMap.values().forEach(ChaosNode::setup);
                cluster.values().forEach(ChaosNode::setup);
            }

            this.restart = restart;
            if (this.restart) {
                log.info("Cluster shutdown...");
                cluster.values().forEach(ChaosNode::teardown);
                metaNodesMap.values().forEach(ChaosNode::stop);
                log.info("Wait for all nodes to shutdown...");
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    log.error("", e);
                }

                log.info("Cluster start...");
                log.info("Wait for all nodes to start...");

                metaNodesMap.values().forEach(ChaosNode::start);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(20));
                } catch (InterruptedException e) {
                    log.error("", e);
                }
                cluster.values().forEach(ChaosNode::start);
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(40));
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }

            if (driverConfiguration.metaNodesParticipateInFault) {
                Map<String, ChaosNode> allNodes = new HashMap<>(metaNodesMap);
                allNodes.putAll(cluster);
                return allNodes;
            } else {
                return cluster;
            }

        } catch (Exception e) {
            log.error("KV model setupCluster failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override public boolean probeCluster() {
        return true;
    }

    @Override public void start() {
        log.info("Start all clients...");
        workers.forEach(Thread::start);
    }

    @Override public void stop() {
        log.info("KV chaos test stop...");
        workers.forEach(Worker::breakLoop);
    }

    @Override public void afterStop() {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("clients is empty");
        } else {
            clients.get(0).lastInvoke();
        }
    }

    @Override public void shutdown() {
        log.info("Teardown client...");
        clients.forEach(Client::teardown);
        if (this.restart) {
            log.info("Stop cluster...");
            cluster.values().forEach(ChaosNode::teardown);
            metaNodesMap.values().forEach(ChaosNode::teardown);
        }
        if (driver != null) {
            driver.shutdown();
        }
    }

    @Override
    public String getStateName() {
        return driver.getStateName();
    }
    
    @Override
    public String getMetaNode() {
        return driver.getMetaNode();
    }

    @Override
    public String getMetaName() {
        return driver.getMetaName();
    }

}
