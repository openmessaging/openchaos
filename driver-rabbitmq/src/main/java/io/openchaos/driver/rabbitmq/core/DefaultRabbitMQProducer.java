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
package io.openchaos.driver.rabbitmq.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DefaultRabbitMQProducer {
    private static final Logger log = LoggerFactory.getLogger(DefaultRabbitMQProducer.class);
    private ConnectionFactory factory;
    private ObjectPool<Channel> channelPool;
    private Connection connection;
    private Channel channel;

    public DefaultRabbitMQProducer() {

    }

    public DefaultRabbitMQProducer(ConnectionFactory factory, Connection connection, ObjectPool<Channel> channelPool) {
        this.factory = factory;
        this.connection = connection;
        this.channelPool = channelPool;
    }

    public void init() {
        try {
            channel = channelPool.borrowObject();
        } catch (Exception e) {
            log.error("borrow channel failed");
        }
    }

    public void sendMessage(String queueName, byte[] message) throws Exception {
        if (channel == null || !channel.isOpen()) {
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

    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            log.warn("Close connection failed!");
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public Connection getNewConnection() {
        try {
            if (connection == null || !connection.isOpen()) {
                connection = factory.newConnection("openchaos_producer");
            }
        } catch (Exception e) {
            log.warn("Create Connection failed");
        }
        return connection;
    }

    public Connection getConnection() {
        return connection;
    }
}

