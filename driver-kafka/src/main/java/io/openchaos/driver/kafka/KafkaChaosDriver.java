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

package io.openchaos.driver.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.kafka.config.KafkaBrokerConfig;
import io.openchaos.driver.kafka.config.KafkaClientConfig;
import io.openchaos.driver.kafka.config.KafkaConfig;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueueDriver;
import io.openchaos.driver.queue.QueueNode;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.queue.QueuePushConsumer;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.admin.AdminClient;
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

public class KafkaChaosDriver implements QueueDriver {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(KafkaChaosDriver.class);
    private KafkaConfig kafkaConfig;
    private KafkaClientConfig kafkaClientConfig;
    private KafkaBrokerConfig kafkaBrokerConfig;
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

    private static KafkaBrokerConfig readConfigForBroker(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, KafkaBrokerConfig.class);
    }

    @Override
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.kafkaConfig = readConfigForKafka(configurationFile);
        this.kafkaBrokerConfig = readConfigForBroker(configurationFile);
        this.kafkaClientConfig = readConfigForClient(configurationFile);

        Properties commonProperties = new Properties();
        commonProperties.load(new StringReader(kafkaClientConfig.commonConfig));

        topicProperties = new Properties();
        commonProperties.forEach((key, value) -> topicProperties.put(key, value));
        topicProperties.load(new StringReader(kafkaClientConfig.topicConfig));

        producerProperties = new Properties();
        commonProperties.forEach((key, value) -> producerProperties.put(key, value));
        producerProperties.load(new StringReader(kafkaClientConfig.producerConfig));
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        consumerProperties = new Properties();
        commonProperties.forEach((key, value) -> consumerProperties.put(key, value));
        consumerProperties.load(new StringReader(kafkaClientConfig.consumerConfig));
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
    }

    @Override
    public void createTopic(String topic, int partitions) {
        try {
            List list = new ArrayList();
            list.add(new NewTopic(topic, partitions, kafkaClientConfig.replicationFactor));
            admin = AdminClient.create(topicProperties);
            admin.createTopics(list).all().get();
        } catch (Exception e) {
            log.error("");
            throw new RuntimeException(String.format("Failed to create topic [%s] to cluster", topic), e);
        }

    }

    @Override
    public QueueProducer createProducer(String topic) {
        KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProperties);
        return new KafkaChaosProducer(producer, topic);
    }

    @Override
    public QueuePushConsumer createPushConsumer(String topic, String subscriptionName,
                                                  ConsumerCallback consumerCallback) {
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, subscriptionName);
        KafkaConsumer<String, byte[]> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return new KafkaChaosPushConsumer(kafkaConsumer, consumerCallback);
    }

    @Override
    public QueuePullConsumer createPullConsumer(String topic, String subscriptionName) {
        throw new UnsupportedOperationException("Unsupport create a pull consumer currently");
//        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, subscriptionName);
//        KafkaConsumer<String, byte[]> kafkaConsumer = new KafkaConsumer<>(consumerProperties);
//        kafkaConsumer.subscribe(Arrays.asList(topic));
//        return new KafkaChaosPullConsumer(kafkaConsumer);
    }

    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }


    @Override
    public QueueNode createChaosNode(String node, List<String> nodes) {
        return new KafkaChaosNode(node, nodes, kafkaConfig, kafkaBrokerConfig);
    }

    @Override public MetaNode createChaosMetaNode(String node, List<String> nodes) {
        return new KafkaChaosZKNode(node, nodes, kafkaConfig);
    }

    @Override
    public String getStateName() {
        return null;
    }
    
    @Override
    public void shutdown() {
        if (admin != null) {
            admin.close();
        }
    }
}
