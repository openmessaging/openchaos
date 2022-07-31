package io.openchaos.driver.rabbitmq;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueueDriver;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.queue.QueuePushConsumer;
import io.openchaos.driver.rabbitmq.config.RabbitMQBrokerConfig;
import io.openchaos.driver.rabbitmq.config.RabbitMQClientConfig;
import io.openchaos.driver.rabbitmq.config.RabbitMQConfig;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMQDriver implements QueueDriver {
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
    private RabbitMQChaosState state;
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
        this.user = rmqClientConfig.user;
        this.password = rmqClientConfig.password;
        state = new RabbitMQChaosState("", queueName, nodes.get(0), rmqBrokerConfig.haMode, user, password);
        state = new RabbitMQChaosState("", queueName, state.getLeader().iterator().next(), rmqBrokerConfig.haMode, user, password);
        factory = new ConnectionFactory();
        factory.setHost(state.getLeader().iterator().next());
        factory.setUsername(user);
        factory.setPassword(password);
        try {
            Connection connection = factory.newConnection("openchaos_driver");
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
            channelPool = new GenericObjectPool<>(channelPoolFactory);
            Channel channel = channelPool.borrowObject();
            channel.queueDelete(queueName);
            channel.queueDeclare(queueName, false, false, false, null);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {}

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

    }

    @Override
    public QueueProducer createProducer(String topic) {
        return new RabbitMQChaosProducer(factory , queueName);
    }

    @Override
    public QueuePushConsumer createPushConsumer(String topic, String subscriptionName, ConsumerCallback consumerCallback) {
        RabbitMQChaosPushConsumer rabbitMQChaosPushConsumer;
        try {
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, factory.newConnection());
            this.channelPool = new GenericObjectPool<>(channelPoolFactory);
            DefaultRabbitMQPushConsumer pushConsumer = new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallback, subscriptionName, channelPool, channelPool.borrowObject());
            rabbitMQChaosPushConsumer = new RabbitMQChaosPushConsumer(pushConsumer,
                    factory, queueName, subscriptionName, consumerCallback);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return  rabbitMQChaosPushConsumer;
    }

    @Override
    public QueuePullConsumer createPullConsumer(String topic, String subscriptionName) {
        return new RabbitMQChaosPullConsumer(factory, queueName, subscriptionName);
    }

}
