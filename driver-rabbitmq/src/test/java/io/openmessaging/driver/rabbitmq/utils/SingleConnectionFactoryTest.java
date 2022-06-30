package io.openmessaging.driver.rabbitmq.utils;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class SingleConnectionFactoryTest {

    @Test
    public void getConnection() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("tcloud");
        factory.setUsername("guest");
        factory.setPassword("guest");
        assertNotNull(SingleConnectionFactory.getConnection(factory));
        assertNotNull(SingleConnectionFactory.getConnection(factory));
    }
}