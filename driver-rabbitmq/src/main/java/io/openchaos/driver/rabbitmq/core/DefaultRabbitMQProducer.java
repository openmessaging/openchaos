package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DefaultRabbitMQProducer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQProducer.class);
    private ConnectionFactory factory;
    private ObjectPool<Channel> channelPool;
    private Connection connection;
    private Channel channel;

    public DefaultRabbitMQProducer() {

    }

    public DefaultRabbitMQProducer(ConnectionFactory factory) {
        this.factory = factory;
        connection = getNewConnection();
        ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
        this.channelPool = new GenericObjectPool<>(channelPoolFactory);
    }

    public void init() {
        try {
            channel = channelPool.borrowObject();
        } catch (Exception e) {
            log.error("borrow channel failed");
        }
    }

    public void sendMessage(String queueName, byte[] message) throws Exception {
        if (channel == null || !channel.isOpen()){
            channel = channelPool.borrowObject();
        }
        try {
            channel.basicPublish("", queueName, null, message);
        } catch (ShutdownSignalException sse) {
            // possibly check if channel was closed
            // by the time we started action and reasons for
            // closing it
            log.warn("connection or channel is shutdown");
            getNewConnection();
        } catch (IOException ioe) {
            // check why connection was closed
            log.warn("IO was blocked");
        }

    }

    public void shutdown(){
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            log.warn("Close connection failed!");
        }
    }

    public Connection getNewConnection() {
        try {
            if (connection == null || !connection.isOpen()) {
                connection = factory.newConnection();
            }
        } catch (Exception e) {
            log.warn("Create Connection failed");
        }
        return connection;
    }

    public Connection getConnection(){
        return connection;
    }
}

