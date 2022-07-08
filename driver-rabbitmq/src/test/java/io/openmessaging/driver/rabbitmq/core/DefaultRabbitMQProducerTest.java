package io.openmessaging.driver.rabbitmq.core;

import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQProducer;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class DefaultRabbitMQProducerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "guest";
    static String password = "guest";
    static DefaultRabbitMQProducer producer = new DefaultRabbitMQProducer(host, port, user, password);

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
            producer.sendMessage("openchaos_client_test", "hello rabbitmq".getBytes(StandardCharsets.UTF_8));
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