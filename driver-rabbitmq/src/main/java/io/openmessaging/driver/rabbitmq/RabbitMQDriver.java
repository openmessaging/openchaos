package io.openmessaging.driver.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.BaseEncoding;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueueDriver;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.queue.QueuePushConsumer;
import io.openmessaging.driver.rabbitmq.config.RabbitMQBrokerConfig;
import io.openmessaging.driver.rabbitmq.config.RabbitMQClientConfig;
import io.openmessaging.driver.rabbitmq.config.RabbitMQConfig;
import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQPullConsumer;
import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import io.openmessaging.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

public class RabbitMQDriver implements QueueDriver {
    private static final Random RANDOM = new Random();
    private static final Logger log = LoggerFactory.getLogger(RabbitMQDriver.class);
    private RabbitMQClientConfig rmqClientConfig;
    private RabbitMQBrokerConfig rmqBrokerConfig;
    private RabbitMQConfig rmqConfig;
    private List<String> nodes;
    private List<String> metaNodes;
    private String user = "guest";
    private String password = "guest";
    private String queueName = "openchaos_client_1";
    private ConnectionFactory factory;
    private Connection connection;
    private ObjectPool<Channel> channelPool;

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static RabbitMQClientConfig readConfigForClient(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQClientConfig.class);
    }

    private static RabbitMQBrokerConfig readConfigForBroker(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQBrokerConfig.class);
    }

    private static RabbitMQConfig readConfigForRMQ(File configurationFile) throws IOException {
        return MAPPER.readValue(configurationFile, RabbitMQConfig.class);
    }

    private static String getRandomString() {
        byte[] buffer = new byte[5];
        RANDOM.nextBytes(buffer);
        return BaseEncoding.base64Url().omitPadding().encode(buffer);
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
    public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.rmqClientConfig = readConfigForClient(configurationFile);
        this.rmqBrokerConfig = readConfigForBroker(configurationFile);
        this.rmqConfig = readConfigForRMQ(configurationFile);
        this.nodes = nodes;
        factory = new ConnectionFactory();
        factory.setHost(nodes.get(0));
        factory.setUsername(user);
        factory.setPassword(password);
        try {
            connection = factory.newConnection();
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
            channelPool = new GenericObjectPool<>(channelPoolFactory);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
        // todo
    }

    @Override
    public ChaosNode createChaosNode(String node, List<String> nodes) {
        this.nodes = nodes;
        return new RabbitMQChaosNode(node, nodes, rmqConfig, rmqBrokerConfig);
    }

    @Override
    public String getStateName() {
        return "io.openchaos.driver.rabbitmq.RabbitMQChaosState";
    }

    @Override
    public void createTopic(String topic, int partitions) {
        //todo
    }

    @Override
    public QueueProducer createProducer(String topic) {
        return new RabbitMQChaosProducer(nodes.get(0), 5672, user, password, queueName);
    }

    //todo subscriptionName 怎么参与mq消费
    @Override
    public QueuePushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        DefaultRabbitMQPushConsumer pushConsumer = new DefaultRabbitMQPushConsumer(connection, queueName, channelPool, consumerCallback);
        RabbitMQChaosPushConsumer rabbitMQChaosPushConsumer = new RabbitMQChaosPushConsumer(pushConsumer,
                nodes.get(0), 5672, user, password, subscriptionName);
        return  rabbitMQChaosPushConsumer;
    }


    // todo subscriptionName 什么作用，如何参与mq的消费的
    @Override
    public QueuePullConsumer createPullConsumer(String topic, String subscriptionName) {
        DefaultRabbitMQPullConsumer consumer = new DefaultRabbitMQPullConsumer(connection, queueName, channelPool);
        return new RabbitMQChaosPullConsumer(consumer, nodes.get(0),
                5672, user, password, queueName);
    }

}
