package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.queue.ConsumerCallback;
import org.apache.commons.pool2.ObjectPool;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultRabbitMQPushConsumerTest {

    static DefaultRabbitMQPushConsumer consumer;
    static String queueName = "openchaos_client_1";

    static {
        ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);
        Connection conn = Mockito.mock(Connection.class);
        ObjectPool channelPool = Mockito.mock(ObjectPool.class);
        Channel channel = Mockito.mock(Channel.class);
        ConsumerCallback callback = Mockito.mock(ConsumerCallback.class);
        try {
            Mockito.when(channelPool.borrowObject()).thenReturn(channel);
            Mockito.when(channel.isOpen()).thenReturn(true);
            Mockito.when(conn.isOpen()).thenReturn(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        consumer = new DefaultRabbitMQPushConsumer(factory, queueName, callback, "group", channelPool, conn);
    }

    @Test
    public void createNewConnection() {
        consumer.createNewChannel();
    }
}
