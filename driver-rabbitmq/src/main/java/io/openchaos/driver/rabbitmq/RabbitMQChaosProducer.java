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
import io.openchaos.common.InvokeResult;
import io.openchaos.driver.queue.QueueProducer;
import io.openchaos.driver.rabbitmq.core.DefaultRabbitMQProducer;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQChaosProducer implements QueueProducer {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQChaosProducer.class);
    String queueName = "openchaos_client_1";
    private DefaultRabbitMQProducer producer;

    public RabbitMQChaosProducer(ConnectionFactory factory, String queueName, Connection connection, ObjectPool<Channel> channelPool) {
        if (notNull(queueName)) {
            this.queueName = queueName;
        }
        producer = new DefaultRabbitMQProducer(factory, connection, channelPool);
    }

    @Override
    public void start() {
        try {
            producer.init();
        } catch (Exception e) {
            log.warn("start RabbitMQ Producer failed");
        }
    }

    @Override
    public void close() {
        producer.shutdown();
        log.warn("Closing RabbitMQProducer.");
    }

    @Override
    public InvokeResult enqueue(byte[] payload) {
        try {
            producer.sendMessage(queueName, payload);
        } catch (IOException e) {
            log.warn("Enqueue fail");
            return InvokeResult.FAILURE;
        } catch (TimeoutException e) {
            log.warn("Enqueue timeout");
            return InvokeResult.FAILURE;
        } catch (Exception e) {
            return InvokeResult.FAILURE;
        }
        return InvokeResult.SUCCESS.setExtraInfoAndReturnSelf(new String(payload));
    }

    @Override
    public InvokeResult enqueue(String shardingKey, byte[] payload) {
        return enqueue(payload);
    }

    private boolean notNull(String s) {
        return s != null && !s.equals("");
    }

}
