package io.openchaos.driver.rabbitmq;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.common.InvokeResult;
import org.apache.commons.pool2.ObjectPool;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class RabbitMQChaosProducerTest {
    ;
    static RabbitMQChaosProducer mqChaosProducer;

    static {
        ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);
        Connection conn = Mockito.mock(Connection.class);
        ObjectPool<Channel> channelPool = Mockito.mock(ObjectPool.class);
        Channel channel = Mockito.mock(Channel.class);
        try {
            Mockito.when(channelPool.borrowObject()).thenReturn(channel);
            Mockito.when(channel.isOpen()).thenReturn(true);
            Mockito.when(conn.isOpen()).thenReturn(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        mqChaosProducer = new RabbitMQChaosProducer(factory, "mqChaosProducer.queueName", conn, channelPool);
    }


    @Test
    public void enqueue() {

        assertEquals(InvokeResult.SUCCESS, mqChaosProducer.enqueue("hello RabbitMQ".getBytes(StandardCharsets.UTF_8)));
    }
}