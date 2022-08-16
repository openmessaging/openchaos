package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.ObjectPool;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;

public class DefaultRabbitMQProducerTest {

    static DefaultRabbitMQProducer producer;

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
        producer = new DefaultRabbitMQProducer(factory, conn, channelPool);
    }


    @Test
    public void init() {
        producer.init();
    }

    @Test
    public void sendMessage() {
        try {
            producer.sendMessage("openchaos_client_1", "hello rabbitmq".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNewConnection() {
        assertNotNull(producer.getNewConnection());
    }

    @Test
    public void shutdown() {
        producer.shutdown();
    }
}