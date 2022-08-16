/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openchaos.driver.rabbitmq;

import com.google.common.collect.Lists;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.QueuePullConsumer;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RabbitMQChaosPullConsumer implements QueuePullConsumer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosPushConsumer.class);
    private ConnectionFactory factory;
    private Connection connection;
    private ObjectPool<Channel> channelPool;
    private String queueName = "openchaos_client_1";
    private Channel channel;
    private String consumeGroup;

    public RabbitMQChaosPullConsumer(ConnectionFactory factory, String queueName, String consumeGroup, ObjectPool<Channel> channelPool, Connection connection) {
        this.queueName = queueName;
        this.consumeGroup = consumeGroup;
        this.factory = factory;
        this.connection = connection;
        this.channelPool = channelPool;
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
