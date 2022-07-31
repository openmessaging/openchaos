package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.ConsumerCallback;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitMQPushConsumer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQPushConsumer.class);
    private Connection connection;
    private String queueName;
    private ObjectPool<Channel> channelPool;
    private ConsumerCallback consumerCallback;
    private Channel channel;
    private ConnectionFactory factory;
    private String consumerGroup;

    public DefaultRabbitMQPushConsumer(ConnectionFactory factory, String queueName,
                                       ConsumerCallback consumerCallback,
                                       String consumerGroup, ObjectPool<Channel> channelPool, Channel channel) {
        this.channel = channel;
        this.channelPool = channelPool;
        this.factory = factory;
        this.queueName = queueName;
        try {
            connection = factory.newConnection(consumerGroup);
            channel.queueDeclare(queueName, false, false,false, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        this.consumerCallback = consumerCallback;
        this.consumerGroup = consumerGroup;

    }

    public void createNewChannel() {
        try {
            if (channel == null || !channel.isOpen()) {
                channel = channelPool.borrowObject();
            }
            channel.basicQos(400);
            channel.basicConsume(queueName, false, "openchaos_client",
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag,
                                                   Envelope envelope,
                                                   AMQP.BasicProperties properties,
                                                   byte[] body) {
                            try {
                                consumerCallback.messageReceived(new Message(body));
                                if (channel == null || !channel.isOpen()) {
                                    channel = channelPool.borrowObject();
                                }
                                channel.basicAck(envelope.getDeliveryTag(), false);
                            } catch (Exception e) {
                                log.warn("Create channel failed");
                            }
                        }
                    });
        } catch (Exception e) {
            log.warn("Connection occured error! Try to create new connection.");
            if (!connection.isOpen()){
                try {
                    connection = factory.newConnection(consumerGroup);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (TimeoutException ex) {
                    throw new RuntimeException(ex);
                }
            }
            createNewChannel();
        }
    }


}


