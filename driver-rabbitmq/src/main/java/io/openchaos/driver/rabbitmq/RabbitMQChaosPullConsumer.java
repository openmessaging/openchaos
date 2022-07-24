package io.openchaos.driver.rabbitmq;

import com.google.common.collect.Lists;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.QueuePullConsumer;
import io.openchaos.driver.rabbitmq.utils.ChannelPoolFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RabbitMQChaosPullConsumer implements QueuePullConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosPushConsumer.class);
    private ConnectionFactory factory;
    private Connection connection;
    private ObjectPool<Channel> channelPool;
    private String queueName = "openchaos_client_1";
    private Channel channel;
    private String consumeGroup;

    public RabbitMQChaosPullConsumer(ConnectionFactory factory, String queueName, String consumeGroup) {
        this.queueName = queueName;
        this.consumeGroup = consumeGroup;
        this.factory = factory;
        try {
            connection = factory.newConnection(consumeGroup);
        } catch (IOException e) {
            log.warn("IO blocked!");
        } catch (TimeoutException e) {
            log.warn("Create connection timeout!");
        }
        ChannelPoolFactory channelPoolFactory = new ChannelPoolFactory(factory, connection);
        channelPool = new GenericObjectPool<>(channelPoolFactory);
    }

    @Override
    public void start() {
        try {
            channel = channelPool.borrowObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    @Override
    public List<Message> dequeue() {
        List<GetResponse> list = poll();
        return Lists.transform(list, messageExt -> new Message("0", messageExt.getBody(),
                messageExt.getProps().getTimestamp().getTime(), System.currentTimeMillis(), buildExtraInfo(messageExt, consumeGroup)));
    }

    private String buildExtraInfo(GetResponse message, String group) {
        return "receive status [ msgId = " + message.getProps().getMessageId() +
                ", topic = " + message.getEnvelope().getExchange() + ", group = " + group + ", queueId = "
                + message.getEnvelope().getRoutingKey() + ", queueOffset = " + message.getMessageCount() + "]";
    }

    public List<GetResponse> poll() {
        List<GetResponse> list = new ArrayList<>();
        try {
            if (channel == null || !channel.isOpen()) {
                channel = channelPool.borrowObject();
            }
            boolean finish = false;
            do {
                GetResponse response = channel.basicGet(queueName, true);
                if (response == null) {
                    finish = true;
                }
                list.add(response);
            } while (!finish);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Connection getConnection() {
        return connection;
    }

}
