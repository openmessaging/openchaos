package io.openmessaging.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.queue.QueuePushConsumer;
import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import io.openmessaging.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQChaosPushConsumer implements QueuePushConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosPushConsumer.class);
    private DefaultRabbitMQPushConsumer consumer;
    private String host = "127.0.0.1";
    private int port = 5672;
    private String user = "guest";
    private String password = "guest";
    private ConnectionFactory factory;
    private Connection connection;
    private ChannelPoolFactory channelPoolFactory;
    private ObjectPool<Channel> channelPool;
    private String queueName = "openchaos_client_1";

    public RabbitMQChaosPushConsumer(DefaultRabbitMQPushConsumer consumer,
                                     String host, int port, String user, String password, String queueName) {
        this.consumer = consumer;
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.queueName = queueName;
    }

    @Override
    public void start() {
        factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        try {
            connection = factory.newConnection();
        } catch (IOException e) {
            log.warn("IO blocked!");
        } catch (TimeoutException e) {
            log.warn("Create connection timeout!");
        }
        channelPoolFactory = new ChannelPoolFactory(factory, connection);
        channelPool = new GenericObjectPool(channelPoolFactory);
        try {
            consumer = new DefaultRabbitMQPushConsumer(connection, queueName, channelPool);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        consumer.createNewConnection();
    }

    @Override
    public void close() {
        channelPool.close();
        try {
            connection.close();
        } catch (IOException e) {
            log.warn("Close connection failed!");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
