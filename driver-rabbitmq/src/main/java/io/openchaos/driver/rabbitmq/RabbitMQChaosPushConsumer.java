package io.openchaos.driver.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.queue.QueuePushConsumer;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQPushConsumer;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class RabbitMQChaosPushConsumer implements QueuePushConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosPushConsumer.class);
    private DefaultRabbitMQPushConsumer consumer;
    private ConnectionFactory factory;
    private Connection connection;
    private ObjectPool<Channel> channelPool;
    private String queueName = "openchaos_client_1";
    private String consumeGroup;
    private ConsumerCallback consumerCallBack;

    public RabbitMQChaosPushConsumer(DefaultRabbitMQPushConsumer consumer,
                                     ConnectionFactory factory, String queueName,
                                     String consumeGroup,
                                     ConsumerCallback consumerCallback) {
        this.consumer = consumer;
        this.factory = factory;
        this.queueName = queueName;
        this.consumeGroup = consumeGroup;
        this.consumerCallBack = consumerCallback;
    }

    @Override
    public void start() {
        try {
            if (consumer == null) {
                consumer = new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallBack, consumeGroup);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        consumer.createNewChannel();
    }

    @Override
    public void close() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            log.warn("Close connection failed!");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
