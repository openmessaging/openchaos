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

package io.openchaos.driver.rocketmq;

import com.google.common.collect.Lists;
import io.openchaos.common.Message;
import io.openchaos.driver.queue.QueuePullConsumer;
import java.util.List;
import org.apache.rocketmq.client.consumer.DefaultLitePullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RocketMQChaosPullConsumer implements QueuePullConsumer {

    private static final Logger log = LoggerFactory.getLogger(RocketMQChaosPullConsumer.class);
    private DefaultLitePullConsumer defaultLitePullConsumer;

    public RocketMQChaosPullConsumer(DefaultLitePullConsumer defaultLitePullConsumer) {
        this.defaultLitePullConsumer = defaultLitePullConsumer;
    }

    @Override public List<Message> dequeue() {
        List<MessageExt> messages = defaultLitePullConsumer.poll();
        if (!messages.isEmpty()) {
            defaultLitePullConsumer.commitSync();
            return Lists.transform(messages, messageExt -> new Message(messageExt.getKeys(), messageExt.getBody(),
                messageExt.getBornTimestamp(), System.currentTimeMillis(), buildExtraInfo(messageExt, defaultLitePullConsumer.getConsumerGroup())));
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
