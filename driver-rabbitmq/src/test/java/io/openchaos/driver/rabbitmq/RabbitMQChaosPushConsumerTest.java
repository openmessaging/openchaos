package io.openchaos.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class RabbitMQChaosPushConsumerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "root";
    static String password = "root";
    static ConnectionFactory factory = new ConnectionFactory();
    static Connection connection ;
    static String queueName = "openchaos_client_1";
    static ObjectPool<Channel> channelPool;
    static RabbitMQChaosPushConsumer consumer;
    static {
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        ConsumerCallback consumerCallback = new ConsumerCallback() {
            @Override
            public void messageReceived(Message message) {
            }
        };
        try {
            connection = factory.newConnection();
            ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
            channelPool = new GenericObjectPool<>(channelPoolFactory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        DefaultRabbitMQPushConsumer defaultRabbitMQPushConsumer = new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallback, "test");
        consumer = new RabbitMQChaosPushConsumer(defaultRabbitMQPushConsumer, factory, queueName, "test", consumerCallback);
    }


    @Test
    public void start() {
        consumer.start();
        assertTrue(consumer.getConnection().isOpen());
    }

    @Test
    public void close() {
        consumer.start();
        consumer.close();
        assertFalse(consumer.getConnection().isOpen());
    }
}