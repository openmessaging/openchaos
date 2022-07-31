package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.checkerframework.checker.units.qual.C;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitMQPushConsumerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "root";
    static String password = "root";
    static ConnectionFactory factory = new ConnectionFactory();
    static Connection connection ;
    static String queueName = "openchaos_client_1";
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

    static DefaultRabbitMQPushConsumer consumer;

    static {
        try {
            consumer = new DefaultRabbitMQPushConsumer(factory, queueName, new ConsumerCallback() {
                @Override
                public void messageReceived(Message message) {

                }
            }, "", channelPool, channelPool.borrowObject());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void createNewConnection() {
        consumer.createNewChannel();
    }
}