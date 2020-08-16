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
import io.openmessaging.chaos.client.CacheClient;
import io.openmessaging.chaos.client.Client;
import io.openmessaging.chaos.driver.cache.CacheChaosDriver;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheModel implements Model {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(CacheModel.class);
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private List<Client> clients;
    private Optional<String> key;
    private List<ClientWorker> workers;
    private Map<String, MQChaosNode> cluster;
    private RateLimiter rateLimiter;
    private int concurrency;
    private Recorder recorder;
    private CacheChaosDriver cacheChaosDriver;
    private File driverConfigFile;

    public CacheModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile) {
        this.concurrency = concurrency;
        this.clients = new ArrayList<>();
        this.workers = new ArrayList<>();
        this.cluster = new HashMap<>();
        this.rateLimiter = rateLimiter;
        this.recorder = recorder;
        this.driverConfigFile = driverConfigFile;
        this.key = Optional.ofNullable(String.format("%s-chaos-topic", DATE_FORMAT.format(new Date())));
    }

    private static CacheChaosDriver createCacheChaosDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = MAPPER.readValue(driverConfigFile, DriverConfiguration.class);
        log.info("Initial driver: {}", WRITER.writeValueAsString(driverConfiguration));

        CacheChaosDriver cacheChaosDriver;

        try {
            cacheChaosDriver = (CacheChaosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            cacheChaosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return cacheChaosDriver;
    }

    @Override public void setupClient() {
        try {
            if (cacheChaosDriver == null) {
                cacheChaosDriver = createCacheChaosDriver(driverConfigFile);
            }

            log.info("Cache clients setup..");

            for (int i = 0; i < concurrency; i++) {
                Client client = new CacheClient(cacheChaosDriver, recorder, key);
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("cacheClient-" + i, client, rateLimiter, log);
                workers.add(clientWorker);
            }

            log.info("{} mq clients setup success", concurrency);

        } catch (Exception e) {
            log.error("Cache model setupClient fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override public Map<String, MQChaosNode> setupCluster(List<String> nodes, boolean isInstall) {
        try {
            if (cacheChaosDriver == null) {
                cacheChaosDriver = createCacheChaosDriver(driverConfigFile);
            }

            if (nodes != null) {
                nodes.forEach(node -> cluster.put(node, cacheChaosDriver.createChaosNode(node, nodes)));
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

    @Override public boolean probeCluster() {
        return true;
    }

    @Override public void start() {
        log.info("Start all clients...");
        workers.forEach(Thread::start);
    }

    @Override public void stop() {
        log.info("MQ chaos test stop");
        workers.forEach(Worker::breakLoop);
    }

    @Override public void afterStop() {
        clients.forEach(Client::lastInvoke);
    }

    @Override public void shutdown() {
        log.info("Teardown client");
        clients.forEach(Client::teardown);
        log.info("Stop cluster");
        cluster.values().forEach(MQChaosNode::stop);
        if (cacheChaosDriver != null) {
            cacheChaosDriver.shutdown();
        }
    }
}
