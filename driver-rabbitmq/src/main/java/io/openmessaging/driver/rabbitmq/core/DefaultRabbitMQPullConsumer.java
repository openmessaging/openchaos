package io.openmessaging.driver.rabbitmq.core;

import com.google.common.collect.Lists;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.GetResponse;
import io.openchaos.common.Message;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultRabbitMQPullConsumer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQPushConsumer.class);
    private Channel channel;
    private Connection connection;
    private  String queueName;
    private ObjectPool<Channel> channelPool;

    public DefaultRabbitMQPullConsumer(Connection connection, String queueName, ObjectPool channelPool){
        this.connection = connection;
        this.queueName = queueName;
        this.channelPool = channelPool;
    }

    public List<GetResponse> poll(){
        List<GetResponse> list = new ArrayList<>();
        try {
            channel = channelPool.borrowObject();
            boolean finish = false;
            do {
                GetResponse response = channel.basicGet(queueName, true);
                if (response == null){
                    finish = true;
                }
                list.add(response);
            } while (finish);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }


}
