package io.openchaos.driver.rabbitmq.utils;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class ChannelPoolFactoryTest {
    static ConnectionFactory factory = init();
    static ChannelPoolFactory channelPoolFactory;

    static {
        try {
            channelPoolFactory = new ChannelPoolFactory(factory, factory.newConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }


    public static ConnectionFactory init() {
        ConnectionFactory factory1 = new ConnectionFactory();
        factory1.setHost("tcloud");
        factory1.setUsername("root");
        factory1.setPassword("root");
        return factory1;
    }

    @Test
    public void create() {
        assertNotNull(channelPoolFactory.create());
    }

    @Test
    public void wrap() {
        assertNotNull(channelPoolFactory.wrap(channelPoolFactory.create()));
    }

    @Test
    public void destroyObject() throws Exception {
        Channel channel = (Channel) channelPoolFactory.create();
        channelPoolFactory.destroyObject(new DefaultPooledObject(channel), DestroyMode.NORMAL);
        assertFalse(channel.isOpen());
    }

    @Test
    public void validateObject() {
        Channel channel = (Channel) channelPoolFactory.create();
        assertTrue(channelPoolFactory.validateObject(new DefaultPooledObject(channel)));
    }
}