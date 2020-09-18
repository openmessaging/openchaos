/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.openmessaging.chaos.driver.rocketmq;

import com.google.common.collect.Lists;
import io.openmessaging.chaos.common.Message;
import io.openmessaging.chaos.driver.mq.MQChaosPullConsumer;
import java.util.List;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosPullConsumer implements MQChaosPullConsumer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQChaosPullConsumer.class);
    private DefaultLitePullConsumer defaultLitePullConsumer;

    public RocketMQChaosPullConsumer(DefaultLitePullConsumer defaultLitePullConsumer) {
        this.defaultLitePullConsumer = defaultLitePullConsumer;
    }

    @Override public List<Message> dequeue() {
        List<MessageExt> messages = defaultLitePullConsumer.poll();
        if (!messages.isEmpty()) {
            defaultLitePullConsumer.commitSync();
            return Lists.transform(messages, messageExt -> new io.openmessaging.chaos.common.Message(messageExt.getKeys(), messageExt.getBody(), buildExtraInfo(messageExt, defaultLitePullConsumer.getConsumerGroup())));
        } else {
            return null;
        }
    }

    @Override public void start() {
        try {
            if (defaultLitePullConsumer != null) {
                defaultLitePullConsumer.start();
            }
        } catch (MQClientException e) {
            log.error("Failed to start the RocketMQChaosPullConsumer instance.", e);
        }
    }

    @Override public void close() {
        if (defaultLitePullConsumer != null) {
            defaultLitePullConsumer.shutdown();
        }
    }

    private String buildExtraInfo(MessageExt message, String group) {
        return "receive status [ msgId = " + message.getMsgId() +
            ", topic = " + message.getTopic() + ", group = " + group + ", queueId = "
            + message.getQueueId() + ", queueOffset = " + message.getQueueOffset() + "]";
    }
}
