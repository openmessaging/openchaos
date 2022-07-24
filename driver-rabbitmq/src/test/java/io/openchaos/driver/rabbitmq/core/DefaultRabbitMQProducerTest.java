package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class DefaultRabbitMQProducerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "root";
    static String password = "root";
    static ConnectionFactory factory;
    static DefaultRabbitMQProducer producer;
    static {
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        producer = new DefaultRabbitMQProducer(factory);
    }

    {
        producer.init();
    }

    @Test
    public void init() {
        assertNotNull(producer.getConnection());
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
    public void shutdown(){
        assertTrue(producer.getConnection().isOpen());
        producer.shutdown();
        assertFalse(producer.getConnection().isOpen());
    }
}