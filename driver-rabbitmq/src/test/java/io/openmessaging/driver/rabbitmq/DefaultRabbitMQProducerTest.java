package io.openmessaging.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.server.ExportException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class DefaultRabbitMQProducerTest {
    static ConnectionFactory factory;
    static String host = "tcloud";
    static int port = 5672;
    static String user = "guest";
    static String password = "guest";
    static DefaultRabbitMQProducer producer = new DefaultRabbitMQProducer(host, port, user, password);
    @Test
    public void init() {
        producer.init();
    }

    @Test
    public void sendMessage() {
        producer.init();
        try {
            producer.sendMessage("openchaos_client_test", "test send message".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        DefaultRabbitMQProducerTest d = new DefaultRabbitMQProducerTest();
        d.init();
        d.sendMessage();
    }
}