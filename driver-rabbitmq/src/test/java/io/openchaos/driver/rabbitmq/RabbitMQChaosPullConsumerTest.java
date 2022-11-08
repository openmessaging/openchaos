package io.openchaos.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

public class RabbitMQChaosPullConsumerTest {
    static RabbitMQChaosPullConsumer consumer;

    static {
        ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);
        Connection conn = Mockito.mock(Connection.class);
        ObjectPool<Channel> channelPool = Mockito.mock(ObjectPool.class);
        Channel channel = Mockito.mock(Channel.class);
        try {
            Mockito.when(channelPool.borrowObject()).thenReturn(channel);
            Mockito.when(channel.isOpen()).thenReturn(true);
            Mockito.when(conn.isOpen()).thenReturn(true);
            Mockito.when(channel.basicGet(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        consumer = new RabbitMQChaosPullConsumer(factory, "queuename", "group", channelPool, conn);
    }


    @Test
    public void close() {
        consumer.start();
        consumer.close();
    }

    @Test
    public void dequeue() throws Exception {
        consumer.start();
        assertEquals(1, consumer.dequeue().size());
    }
}