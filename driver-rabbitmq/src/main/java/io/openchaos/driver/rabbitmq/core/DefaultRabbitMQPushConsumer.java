package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
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

public class DefaultRabbitMQPushConsumer implements Consumer {
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
                                       String consumerGroup) {
        this.factory = factory;
        this.queueName = queueName;
        try {
            connection = factory.newConnection(consumerGroup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
        this.channelPool = new GenericObjectPool<>(channelPoolFactory);
        this.consumerCallback = consumerCallback;
        this.consumerGroup = consumerGroup;
        try {
            channel = channelPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void handleConsumeOk(String s) {

    }

    @Override
    public void handleCancelOk(String s) {

    }

    @Override
    public void handleCancel(String s) throws IOException {
        log.warn("Handle cancel : " + s);
        createNewChannel();
    }

    @Override
    public void handleShutdownSignal(String s, ShutdownSignalException e) {
        log.warn("HandleShutdownSignal : " + s + e.getReason());
        createNewChannel();
    }

    @Override
    public void handleRecoverOk(String s) {

    }

    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) {
        try {
            consumerCallback.messageReceived(new Message(bytes));
            if (channel == null || !channel.isOpen()) {
                channel = channelPool.borrowObject();
            }
            channel.basicAck(envelope.getDeliveryTag(), false);
        } catch (Exception e) {
            log.warn("Create channel failed");
        }
    }

    public void createNewChannel() {
        try {
            Thread.sleep(1000);
            if (channel == null || !channel.isOpen()) {
                channel = channelPool.borrowObject();
            }
            channel.basicQos(64);
            channel.basicConsume(queueName, false, "openchaos_client", new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallback, consumerGroup));
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
