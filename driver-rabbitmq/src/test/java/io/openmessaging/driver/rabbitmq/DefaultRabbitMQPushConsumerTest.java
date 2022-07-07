package io.openmessaging.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import io.openmessaging.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitMQPushConsumerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "guest";
    static String password = "guest";
    static ConnectionFactory factory = new ConnectionFactory();
    static Connection connection ;
    static String queueName = "openchaos_client_test";
    static ObjectPool<Channel> channelPool;
    static {
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        try {
            connection = factory.newConnection();
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
            channelPool = new GenericObjectPool<>(channelPoolFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    static DefaultRabbitMQPushConsumer consumer = new DefaultRabbitMQPushConsumer(connection, queueName, channelPool);


    @Test
    public void createNewConnection() {
        consumer.createNewConnection();
    }
}