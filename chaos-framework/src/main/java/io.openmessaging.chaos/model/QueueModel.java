/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
import io.openmessaging.chaos.driver.MQChaosDriver;
import io.openmessaging.chaos.driver.MQChaosNode;
import io.openmessaging.chaos.recorder.Recorder;
import io.openmessaging.chaos.recorder.RequestLogEntry;
import io.openmessaging.chaos.utils.ListPartition;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The model for mq test
 */
public class QueueModel implements Model {

    private List<Client> clients;

    private List<ClientWorker> workers;

    private Map<String, MQChaosNode> cluster;

    private Recorder recorder;

    private File driverConfigFile;

    private int concurrency;

    private RateLimiter rateLimiter;

    private MQChaosDriver mqChaosDriver;

    private String chaosTopic;

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private static final Logger logger = LoggerFactory.getLogger(QueueModel.class);

    private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public QueueModel(int concurrency, RateLimiter rateLimiter, Recorder recorder, File driverConfigFile) {
        this.concurrency = concurrency;
        this.recorder = recorder;
        this.driverConfigFile = driverConfigFile;
        this.rateLimiter = rateLimiter;
        clients = new ArrayList<>();
        workers = new ArrayList<>();
        cluster = new HashMap<>();
        chaosTopic = String.format("%s-chaos-topic", dateFormat.format(new Date()));
    }

    @Override public Map<String, MQChaosNode> setupCluster(List<String> nodes, boolean isInstall) {
        try {
            if (mqChaosDriver == null) {
                mqChaosDriver = createChaosMQDriver(driverConfigFile);
            }

            if(nodes!=null){
                nodes.forEach(node -> cluster.put(node, mqChaosDriver.createChaosNode(node, nodes)));
            }

            if (isInstall) {
//                cluster.values().forEach(MQChaosNode::setup);
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                for(MQChaosNode node:cluster.values()){
                    futures.add(node.setup());
                }
                futures.forEach(CompletableFuture::join);
            }

            logger.info("Cluster shutdown");
            cluster.values().forEach(MQChaosNode::teardown);
            logger.info("Wait for all nodes to shutdown...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            }catch (InterruptedException e){
                logger.error("",e);
            }

            logger.info("Cluster start...");
            cluster.values().forEach(MQChaosNode::start);
            logger.info("Wait for all nodes to start...");
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(20));
            }catch (InterruptedException e){
                logger.error("",e);
            }

            return cluster;
        } catch (Exception e) {
            logger.error("Queue model setupCluster fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override public void setupClient(boolean isOrderTest, List<String> shardingKeys) {
        try {
            if(mqChaosDriver == null) {
                mqChaosDriver = createChaosMQDriver(driverConfigFile);
            }

            logger.info("Create chaos topic : {}", chaosTopic);
            mqChaosDriver.createTopic(chaosTopic, 8);

            logger.info("MQ clients setup..");

            List<List<String>> shardingKeyLists = ListPartition.partitionList(shardingKeys, concurrency);
            for (int i = 0; i < concurrency; i++) {
                Client client = new QueueClient(mqChaosDriver, chaosTopic, recorder, isOrderTest, shardingKeyLists.get(i));
                client.setup();
                clients.add(client);
                ClientWorker clientWorker = new ClientWorker("queueClient-" + i, client, rateLimiter, logger);
                workers.add(clientWorker);
            }

            logger.info("{} mq clients setup success", concurrency);
        } catch (Exception e) {
            logger.error("Queue model setupClient fail", e);
            throw new RuntimeException(e);
        }
    }

    @Override public void start() {
        logger.info("Start all clients...");
        workers.forEach(Thread::start);
    }

    @Override public void stop() {
        logger.info("MQ chaos test stop");
        workers.forEach(Worker::breakLoop);
    }

    @Override public void afterStop() {
        logger.info("Invoke drain");
        clients.forEach(Client::lastInvoke);
    }

    @Override public void shutdown() {
        logger.info("Close mq client");
        clients.forEach(Client::teardown);
        logger.info("Teardown mq cluster");
        cluster.values().forEach(MQChaosNode::teardown);
        mqChaosDriver.close();
    }

    private static MQChaosDriver createChaosMQDriver(File driverConfigFile) throws IOException {

        DriverConfiguration driverConfiguration = mapper.readValue(driverConfigFile, DriverConfiguration.class);
        logger.info("Initial driver: {}", writer.writeValueAsString(driverConfiguration));

        MQChaosDriver mqChaosDriver;

        try {
            mqChaosDriver = (MQChaosDriver) Class.forName(driverConfiguration.driverClass).newInstance();
            mqChaosDriver.initialize(driverConfigFile, driverConfiguration.nodes);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return mqChaosDriver;
    }

    public static void main(String[] args) throws Exception{
        System.out.println(writer.writeValueAsString(new RequestLogEntry(1,"123","123",123)));
    }
}
