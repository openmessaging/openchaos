package io.openmessaging.chaos.driver.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openmessaging.chaos.driver.kafka.config.KafkaBrokerConfig;
import io.openmessaging.chaos.driver.kafka.config.KafkaClientConfig;
import io.openmessaging.chaos.driver.kafka.config.KafkaConfig;
import io.openmessaging.chaos.driver.mq.*;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class KafkaChaosDriver implements MQChaosDriver {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(KafkaChaosDriver.class);
    private KafkaClientConfig kafkaClientConfig;
    private KafkaConfig kafkaConfig;
    private KafkaBrokerConfig kafkaBrokerConfig;
    private List<String> nodes;

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
        this.kafkaClientConfig = readConfigForClient(configurationFile);
        this.kafkaConfig = readConfigForKafka(configurationFile);
        this.kafkaBrokerConfig = readBrokerConfigForKafka(configurationFile);
        this.nodes = nodes;
    }

    @Override
    public void createTopic(String topic, int partitions) {
        try {
            Properties properties = new Properties();
            properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBroker(nodes));
            List list = new ArrayList();
            list.add(new NewTopic(topic, partitions, (short) (nodes.size() - 1)));
            AdminClient.create(properties).createTopics(list);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to create topic [%s] to cluster", topic), e);
        }

    }

    @Override
    public MQChaosProducer createProducer(String topic) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBroker(nodes));
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        return new KafkaChaosProducer(producer, topic);
    }

    @Override
    public MQChaosPushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        return new KafkaChaosPushConsumer();
    }

    @Override
    public MQChaosPullConsumer createPullConsumer(String topic, String subscriptionName) {
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBroker(nodes));
        properties.put("group.id", subscriptionName);
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("session.timeout.ms", "30000");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(Arrays.asList(topic));
        return new KafkaChaosPullConsumer(kafkaConsumer);
    }


    @Override
    public MQChaosNode createChaosNode(String node, List<String> nodes) {
        return new KafkaChaosNode(node, nodes, kafkaConfig, kafkaBrokerConfig);
    }


    @Override
    public void shutdown() {
    }

    private String getBroker(List<String> nodes) {
        StringBuilder res = new StringBuilder();
        nodes.forEach(node -> res.append(node + ":9092,"));
        return res.toString().substring(0, res.length() - 1);
    }
}
