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

package io.openmessaging.chaos.driver.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.chaos.driver.kafka.config.KafkaBrokerConfig;
import io.openmessaging.chaos.driver.kafka.config.KafkaClientConfig;
import io.openmessaging.chaos.driver.kafka.config.KafkaConfig;
import io.openmessaging.chaos.driver.mq.ConsumerCallback;
import io.openmessaging.chaos.driver.mq.MQChaosDriver;
import io.openmessaging.chaos.driver.mq.MQChaosNode;
import io.openmessaging.chaos.driver.mq.MQChaosProducer;
import io.openmessaging.chaos.driver.mq.MQChaosPullConsumer;
import io.openmessaging.chaos.driver.mq.MQChaosPushConsumer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaChaosDriver implements MQChaosDriver {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(KafkaChaosDriver.class);
    private KafkaConfig kafkaConfig;
    private KafkaBrokerConfig kafkaBrokerConfig;
    private List<String> nodes;
    private AdminClient admin;
    private Properties topicProperties;
    private Properties producerProperties;
    private Properties consumerProperties;


    private static KafkaClientConfig readConfigForClient(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, KafkaClientConfig.class);
    }

    private static KafkaConfig readConfigForKafka(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, KafkaConfig.class);
    }

    private static KafkaBrokerConfig readBrokerConfigForKafka(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, KafkaBrokerConfig.class);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.kafkaConfig = readConfigForKafka(configurationFile);
        this.kafkaBrokerConfig = readBrokerConfigForKafka(configurationFile);
        this.nodes = nodes;

        topicProperties = new Properties();
        topicProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapBrokers(nodes));

        producerProperties = new Properties();
        producerProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapBrokers(nodes));
        producerProperties.put("acks", "all");
        producerProperties.put("retries", 0);
        producerProperties.put("batch.size", 16384);
        producerProperties.put("linger.ms", 1);
        producerProperties.put("buffer.memory", 33554432);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        consumerProperties = new Properties();
        consumerProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapBrokers(nodes));
        consumerProperties.put("enable.auto.commit", "true");
        consumerProperties.put("auto.commit.interval.ms", "1000");
        consumerProperties.put("session.timeout.ms", "30000");
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    }

    @Override
    public void createTopic(String topic, int partitions) {
        try {
            List list = new ArrayList();
            list.add(new NewTopic(topic, partitions, (short) nodes.size()));
            admin = AdminClient.create(topicProperties);
            admin.createTopics(list).all().get();
        } catch (Exception e) {
            log.error("");
            throw new RuntimeException(String.format("Failed to create topic [%s] to cluster", topic), e);
        }

    }

    @Override
    public MQChaosProducer createProducer(String topic) {
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties);
        return new KafkaChaosProducer(producer, topic);
    }

    @Override
    public MQChaosPushConsumer createPushConsumer(String topic, String subscriptionName,
                                                  ConsumerCallback consumerCallback) {
        consumerProperties.put("group.id", subscriptionName);
        KafkaConsumer<String, byte[]> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return new KafkaChaosPushConsumer(kafkaConsumer, consumerCallback);
    }

    @Override
    public MQChaosPullConsumer createPullConsumer(String topic, String subscriptionName) {
        consumerProperties.put("group.id", subscriptionName);
        KafkaConsumer<String, byte[]> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return new KafkaChaosPullConsumer(kafkaConsumer);
    }

    @Override
    public MQChaosNode createChaosNode(String node, List<String> nodes) {
        return new KafkaChaosNode(node, nodes, kafkaConfig, kafkaBrokerConfig);
    }

    @Override
    public void shutdown() {
        if (admin != null) {
            admin.close();
        }
    }

    private String getBootstrapBrokers(List<String> nodes) {
        StringBuilder res = new StringBuilder();
        nodes.forEach(node -> res.append(node + ":9092,"));
        return res.toString().substring(0, res.length() - 1);
    }
}
