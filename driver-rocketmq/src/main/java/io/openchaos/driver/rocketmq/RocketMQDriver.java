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

package io.openchaos.driver.rocketmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;
import io.openchaos.common.Message;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.PubSubDriver;
import io.openchaos.driver.queue.MQChaosNode;
import io.openchaos.driver.queue.MQChaosProducer;
import io.openchaos.driver.queue.MQChaosPullConsumer;
import io.openchaos.driver.queue.MQChaosPushConsumer;
import io.openchaos.driver.rocketmq.config.RocketMQBrokerConfig;
import io.openchaos.driver.rocketmq.config.RocketMQClientConfig;
import io.openchaos.driver.rocketmq.config.RocketMQConfig;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.TopicConfig;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.command.CommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQDriver implements PubSubDriver {

    private static final Random RANDOM = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(RocketMQDriver.class);
    private DefaultMQAdminExt rmqAdmin;
    private RocketMQClientConfig rmqClientConfig;
    private RocketMQBrokerConfig rmqBrokerConfig;
    private RocketMQConfig rmqConfig;
    private String nameServerPort = "9876";
    private List<String> nodes;
    private List<String> preNodes;

    private static RocketMQClientConfig readConfigForClient(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RocketMQClientConfig.class);
    }

    private static RocketMQBrokerConfig readConfigForBroker(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RocketMQBrokerConfig.class);
    }

    private static RocketMQConfig readConfigForRMQ(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RocketMQConfig.class);
    }

    private static String getRandomString() {
        byte[] buffer = new byte[5];
        RANDOM.nextBytes(buffer);
        return BaseEncoding.base64Url().omitPadding().encode(buffer);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.rmqClientConfig = readConfigForClient(configurationFile);
        this.rmqBrokerConfig = readConfigForBroker(configurationFile);
        this.rmqConfig = readConfigForRMQ(configurationFile);
        this.nodes = nodes;
        if (rmqConfig.nameServerPort != null && !rmqConfig.nameServerPort.isEmpty()) {
            this.nameServerPort = rmqConfig.nameServerPort;
        }
    }

    @Override
    public MQChaosNode createChaosNode(String node, List<String> nodes) {
        this.nodes = nodes;
        return new RocketMQChaosNode(node, nodes, preNodes, rmqConfig, rmqBrokerConfig);
    }

    @Override
    public MQChaosProducer createProducer(String topic) {
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer("ProducerGroup_Chaos");
        defaultMQProducer.setNamesrvAddr(getNameserver());
        defaultMQProducer.setInstanceName("ProducerInstance" + getRandomString());

        return new RocketMQChaosProducer(defaultMQProducer, topic);
    }

    @Override
    public MQChaosPushConsumer createPushConsumer(String topic, String subscriptionName,
        ConsumerCallback consumerCallback) {
        DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer(subscriptionName);
        defaultMQPushConsumer.setNamesrvAddr(getNameserver());
        defaultMQPushConsumer.setInstanceName("ConsumerInstance" + getRandomString());
        try {
            defaultMQPushConsumer.subscribe(topic, "*");
            defaultMQPushConsumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
                for (MessageExt message : msgs) {
                    consumerCallback.messageReceived(new Message(message.getKeys(), message.getBody(), message.getBornTimestamp(), System.currentTimeMillis(), buildExtraInfo(message, subscriptionName)));
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            });
        } catch (MQClientException e) {
            log.error("Failed to create consumer instance.", e);
        }
        return new RocketMQChaosPushConsumer(defaultMQPushConsumer);
    }

    @Override
    public MQChaosPullConsumer createPullConsumer(String topic, String subscriptionName) {
        DefaultLitePullConsumer defaultLitePullConsumer = new DefaultLitePullConsumer(subscriptionName);
        defaultLitePullConsumer.setNamesrvAddr(getNameserver());
        defaultLitePullConsumer.setInstanceName("ConsumerInstance" + getRandomString());
        defaultLitePullConsumer.setPollTimeoutMillis(100);
        defaultLitePullConsumer.setPullBatchSize(5);
        try {
            defaultLitePullConsumer.subscribe(topic, "*");
        } catch (MQClientException e) {
            log.error("Failed to start the created lite pull consumer instance.", e);
        }
        return new RocketMQChaosPullConsumer(defaultLitePullConsumer);
    }

    @Override
    public String getMetaNode() {
        return getNameserver();
    }

    @Override
    public String getMetaName() {
        return this.rmqClientConfig.clusterName;
    }

    @Override
    public void createTopic(String topic, int partitions) {

        this.rmqAdmin = new DefaultMQAdminExt();
        this.rmqAdmin.setNamesrvAddr(getNameserver());
        this.rmqAdmin.setInstanceName("AdminInstance-" + getRandomString());
        try {
            this.rmqAdmin.start();
        } catch (MQClientException e) {
            log.error("Start the RocketMQ admin tool failed.");
        }

        TopicConfig topicConfig = new TopicConfig();
        topicConfig.setOrder(false);
        topicConfig.setPerm(6);
        topicConfig.setReadQueueNums(partitions);
        topicConfig.setWriteQueueNums(partitions);
        topicConfig.setTopicName(topic);

        try {
            Set<String> brokerList = CommandUtil.fetchMasterAddrByClusterName(this.rmqAdmin, this.rmqClientConfig.clusterName);
            topicConfig.setReadQueueNums(Math.max(1, partitions / brokerList.size()));
            topicConfig.setWriteQueueNums(Math.max(1, partitions / brokerList.size()));

            for (String brokerAddr : brokerList) {
                this.rmqAdmin.createAndUpdateTopicConfig(brokerAddr, topicConfig);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create topic [%s] to cluster [%s]", topic, this.rmqClientConfig.clusterName), e);
        }
    }

    public void shutdown() {
        rmqAdmin.shutdown();
    }

    @Override public MetaNode createPreChaosNode(String node, List<String> nodes) {
        preNodes = nodes;
        return new RocketMQMetaNode(node, nodes, rmqConfig);
    }

    @Override
    public String getStateName() {
        return "io.openchaos.driver.rocketmq.RocketMQChaosState";
    }    
    
    private String getNameserver() {
        if (rmqBrokerConfig.namesrvAddr != null && !rmqBrokerConfig.namesrvAddr.isEmpty()) {
            return rmqBrokerConfig.namesrvAddr;
        } else if (preNodes != null) {
            StringBuilder res = new StringBuilder();
            preNodes.forEach(node -> res.append(node + ":" + nameServerPort + ";"));
            return res.toString();
        } else {
            StringBuilder res = new StringBuilder();
            nodes.forEach(node -> res.append(node + ":" + nameServerPort + ";"));
            return res.toString();
        }
    }

    private String buildExtraInfo(MessageExt message, String group) {
        return "receive status [ msgId = " + message.getMsgId() +
            ", topic = " + message.getTopic() + ", group = " + group + ", queueId = "
            + message.getQueueId() + ", queueOffset = " + message.getQueueOffset() + "]";
    }

}
