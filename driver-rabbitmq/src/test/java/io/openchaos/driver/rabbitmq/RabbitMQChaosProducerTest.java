package io.openchaos.driver.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQProducer;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class RabbitMQChaosProducerTest {
    static String host = "tcloud";
    static int port = 5672;
    static String user = "root";
    static String password = "root";
    static String queueName = "openchaos_client_1";
    static ConnectionFactory factory;
    static DefaultRabbitMQProducer producer;
    static RabbitMQChaosProducer mqChaosProducer ;
    static {
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(user);
        factory.setPassword(password);
        producer = new DefaultRabbitMQProducer(factory);
        mqChaosProducer = new RabbitMQChaosProducer(factory, queueName);
    }


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