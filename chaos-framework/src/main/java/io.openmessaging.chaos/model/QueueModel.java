/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.openmessaging.chaos.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openmessaging.chaos.DriverConfiguration;
import io.openmessaging.chaos.client.Client;
import io.openmessaging.chaos.client.QueueClient;
import io.openmessaging.chaos.common.utils.Utils;
import io.openmessaging.chaos.driver.mq.MQChaosDriver;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.worker.ClientWorker;
import io.openmessaging.chaos.worker.Worker;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The model for mq test
 */
public class QueueModel implements Model {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(QueueModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private List<Client> clients;
    private List<ClientWorker> workers;
    private Map<String, MQChaosNode> cluster;
    private Recorder recorder;
    private File driverConfigFile;
    private int concurrency;
    private RateLimiter rateLimiter;
    private MQChaosDriver mqChaosDriver;
    private String chaosTopic;
    private boolean isOrderTest;
    private boolean isUsePull;
    private List<String> shardingKeys;
    private AtomicLong msgReceivedCount = new AtomicLong(0);

    public QueueModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile,
        boolean isOrderTest, boolean isUsePull, List<String> shardingKeys) {
        this.concurrency = concurrency;
        this.recorder = recorder;
        this.driverConfigFile = driverConfigFile;
        this.rateLimiter = rateLimiter;
        clients = new ArrayList<>();
        workers = new ArrayList<>();
        cluster = new HashMap<>();
        chaosTopic = String.format("%s-chaos-topic", DATE_FORMAT.format(new Date()));
        this.isOrderTest = isOrderTest;
        this.isUsePull = isUsePull;
        this.shardingKeys = shardingKeys;
    }

    private static MQChaosDriver createChaosDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        MQChaosDriver mqChaosDriver;

        try {
            mqChaosDriver = (MQChaosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            mqChaosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return mqChaosDriver;
    }

    @Override
    public Map<String, MQChaosNode> setupCluster(List<String> nodes, boolean isInstall) {
        try {
            if (mqChaosDriver == null) {
                mqChaosDriver = createChaosDriver(driverConfigFile);
            }

            if (nodes != null) {
                nodes.forEach(node -> cluster.put(node, mqChaosDriver.createChaosNode(node, nodes)));
            }

            if (isInstall) {
                cluster.values().forEach(MQChaosNode::setup);
            }

            log.info("Cluster shutdown");
            cluster.values().forEach(MQChaosNode::stop);
            log.info("Wait for all nodes to shutdown...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                log.error("", e);
            }

            log.info("Cluster start...");
            cluster.values().forEach(MQChaosNode::start);
            log.info("Wait for all nodes to start...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
            } catch (InterruptedException e) {
                log.error("", e);
            }

            return cluster;
        } catch (Exception e) {
            log.error("Queue model setupCluster fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setupClient() {
        try {
            if (mqChaosDriver == null) {
                mqChaosDriver = createChaosDriver(driverConfigFile);
            }

            log.info("Create chaos topic : {}", chaosTopic);
            mqChaosDriver.createTopic(chaosTopic, 8);

            log.info("MQ clients setup..");

            List<List<String>> shardingKeyLists = Utils.partitionList(shardingKeys, concurrency);
            for (int i = 0; i < concurrency; i++) {
                Client client = new QueueClient(mqChaosDriver, chaosTopic, recorder, isOrderTest, isUsePull, shardingKeyLists.get(i), msgReceivedCount);
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("queueClient-" + i, client, rateLimiter, log);
                workers.add(clientWorker);
            }

            log.info("{} mq clients setup success", concurrency);
        } catch (Exception e) {
            log.error("Queue model setupClient fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        log.info("Start all clients...");
        workers.forEach(Thread::start);
    }

    @Override
    public void stop() {
        log.info("MQ chaos test stop");
        workers.forEach(Worker::breakLoop);
    }

    @Override
    public void afterStop() {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("clients is empty");
        } else {
            clients.get(0).lastInvoke();
        }
    }

    @Override
    public void shutdown() {
        log.info("Teardown client");
        clients.forEach(Client::teardown);
        log.info("Stop cluster");
        cluster.values().forEach(MQChaosNode::stop);
        if (mqChaosDriver != null) {
            mqChaosDriver.shutdown();
        }
    }

    @Override
    public boolean probeCluster() {
        int probeTimes = 0;
        try {
            while (msgReceivedCount.get() == 0) {
                if (++probeTimes == 10) {
                    break;
                }
                clients.forEach(Client::nextInvoke);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("", e);
            return false;
        }
        return probeTimes != 10;
    }
}
