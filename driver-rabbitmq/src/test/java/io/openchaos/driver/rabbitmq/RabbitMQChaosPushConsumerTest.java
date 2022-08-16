package io.openchaos.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import org.apache.commons.pool2.ObjectPool;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;

public class RabbitMQChaosPushConsumerTest {
    static RabbitMQChaosPushConsumer consumer;

    static {
        ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);
        Connection conn = Mockito.mock(Connection.class);
        ObjectPool<Channel> channelPool = Mockito.mock(ObjectPool.class);
        Channel channel = Mockito.mock(Channel.class);
        ConsumerCallback callback = Mockito.mock(ConsumerCallback.class);
        DefaultRabbitMQPushConsumer pushConsumer = Mockito.mock(DefaultRabbitMQPushConsumer.class);
        try {
            Mockito.when(channelPool.borrowObject()).thenReturn(channel);
            Mockito.when(channel.isOpen()).thenReturn(true);
            Mockito.when(conn.isOpen()).thenReturn(true);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        consumer = new RabbitMQChaosPushConsumer(pushConsumer, factory, "queuename",
                "group", callback, channelPool, conn);
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
    }


}