package io.openmessaging.driver.rabbitmq;

import io.openmessaging.driver.rabbitmq.core.DefaultRabbitMQProducer;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class RabbitMQChaosProducerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "guest";
    static String password = "guest";
    static String queueName = "openchaos_client_test";
    static DefaultRabbitMQProducer producer = new DefaultRabbitMQProducer(host, port, user, password);
    static RabbitMQChaosProducer mqChaosProducer = new RabbitMQChaosProducer(host, port, user, password, queueName);

    {
        mqChaosProducer.start();
    }
    @Test
    public void start() {
        assertTrue(mqChaosProducer.getProducer().getConnection().isOpen());
    }

    @Test
    public void close() {
        mqChaosProducer.close();
        assertFalse(mqChaosProducer.getProducer().getConnection().isOpen());
    }

    @Test
    public void enqueue() {
        mqChaosProducer.enqueue("hello RabbitMQ".getBytes(StandardCharsets.UTF_8));
    }
}