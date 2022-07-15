package io.openmessaging.driver.rabbitmq.core;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.ConsumerCallback;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DefaultRabbitMQPushConsumer implements Consumer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQPushConsumer.class);
    private  Connection connection;
    private  String queueName;
    private ObjectPool<Channel> channelPool;
    private ConsumerCallback consumerCallback;
    public DefaultRabbitMQPushConsumer(Connection connection, String queueName, ObjectPool channelPool) {
        this.connection = connection;
        this.queueName = queueName;
        this.channelPool = channelPool;
    }

    public DefaultRabbitMQPushConsumer(Connection connection, String queueName, ObjectPool channelPool, ConsumerCallback consumerCallback) {
        this.connection = connection;
        this.queueName = queueName;
        this.channelPool = channelPool;
        this.consumerCallback = consumerCallback;
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
        createNewConnection();
    }

    @Override
    public void handleShutdownSignal(String s, ShutdownSignalException e) {
        log.warn("HandleShutdownSignal : " + s + e.getReason());
        createNewConnection();
    }

    @Override
    public void handleRecoverOk(String s) {

    }

    @Override
    public void handleDelivery(String s, Envelope envelope, AMQP.BasicProperties basicProperties, byte[] bytes) throws IOException {
        Channel channel = null;
        try {
            consumerCallback.messageReceived(new Message(bytes));
            channel = channelPool.borrowObject();
            channel.basicAck(envelope.getDeliveryTag(), false);
        } catch (Exception e) {
            log.warn("Create channel failed");
        }
    }

    public void createNewConnection(){
        try {
            Thread.sleep(1000);
            Channel channel = channelPool.borrowObject();
            channel.basicQos(64);
            channel.basicConsume(queueName, false, new DefaultRabbitMQPushConsumer(connection, queueName, channelPool));

        } catch (Exception e) {
            log.warn("Connection occured error! Try to create new connection.");
            createNewConnection();
        }
    }

}
