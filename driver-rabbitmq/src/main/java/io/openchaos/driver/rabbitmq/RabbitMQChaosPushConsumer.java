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
                                     ConsumerCallback consumerCallback, ObjectPool<Channel> channelPool, Connection connection) {
        this.consumer = consumer;
        this.factory = factory;
        this.queueName = queueName;
        this.consumeGroup = consumeGroup;
        this.consumerCallBack = consumerCallback;
        this.connection = connection;
        this.channelPool = channelPool;
    }

    @Override
    public void start() {
        try {
            if (consumer == null) {
                consumer = new DefaultRabbitMQPushConsumer(factory, queueName, consumerCallBack, consumeGroup, channelPool, connection);
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

    public DefaultRabbitMQPushConsumer getConsumer() {
        return consumer;
    }
}
