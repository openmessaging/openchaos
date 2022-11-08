package io.openchaos.driver.rabbitmq.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChannelPoolFactoryTest {
    static ConnectionFactory factory = Mockito.mock(ConnectionFactory.class);
    static Connection connection = Mockito.mock(Connection.class);
    static ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);

    static {
        try {
            Mockito.when(factory.newConnection()).thenReturn(Mockito.mock(Connection.class));
            Mockito.when(connection.createChannel()).thenReturn(Mockito.mock(Channel.class));
            Mockito.when(connection.isOpen()).thenReturn(Boolean.TRUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    public void create() throws IOException, TimeoutException {
        assertNotNull(channelPoolFactory.create());
    }

    @Test
    public void wrap() {
        assertNotNull(channelPoolFactory.wrap(Mockito.mock(Object.class)));
    }

    @Test
    public void destroyObject() throws Exception {
        PooledObject mock = Mockito.mock(PooledObject.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(mock.getObject()).thenReturn(channel);
        Mockito.when(channel.isOpen()).thenReturn(true);
        channelPoolFactory.destroyObject(mock, DestroyMode.NORMAL);
    }

    @Test
    public void validateObject() {
        PooledObject mock = Mockito.mock(PooledObject.class);
        Channel channel = Mockito.mock(Channel.class);
        Mockito.when(mock.getObject()).thenReturn(channel);
        Mockito.when(channel.isOpen()).thenReturn(true);
        assertTrue(channelPoolFactory.validateObject(mock));
    }
}