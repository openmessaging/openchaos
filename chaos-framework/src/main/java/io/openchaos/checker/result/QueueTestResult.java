/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.openchaos.checker.result;

import java.util.Map;

public class QueueTestResult extends TestResult {
    public long enqueueInvokeCount;
    public long enqueueSuccessCount;
    public long dequeueSuccessCount;
    public long lostMessageCount;
    public Map<String, String> lostMessages;
    public long duplicateMessageCount;
    public Map<String, String> duplicateMessages;
    public boolean atMostOnce;
    public boolean atLeastOnce;
    public boolean exactlyOnce;

    public QueueTestResult() {
        super("MQTestResult");
    }

    @Override
    public String toString() {
        return "\n" + name + "{" +
            "\n\tenqueueInvokeCount=" + enqueueInvokeCount +
            "\n\tenqueueSuccessCount=" + enqueueSuccessCount +
            "\n\tdequeueSuccessCount=" + dequeueSuccessCount +
            "\n\tlostMessageCount=" + lostMessageCount +
            "\n\tlostMessages=" + formatLostMessages(lostMessages) +
            "\n\tduplicateMessageCount=" + duplicateMessageCount +
            "\n\tduplicateMessages=" + formatDuplicateMessages(duplicateMessages) +
            "\n\tatMostOnce=" + atMostOnce +
            "\n\tatLeastOnce=" + atLeastOnce +
            "\n\texactlyOnce=" + exactlyOnce +
            "\n\tisValid=" + isValid +
            "\n }";
    }

    public String formatLostMessages(Map<String, String> lostMessages) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, String> message : lostMessages.entrySet()) {
            stringBuilder.append("\n\t\t [ lost value = ").append(message.getKey()).append(" , info = ").append(message.getValue()).append(" ]");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public String formatDuplicateMessages(Map<String, String> duplicateMessages) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        for (Map.Entry<String, String> message : duplicateMessages.entrySet()) {
            stringBuilder.append("\n\t\t [ duplicate value = ").append(message.getKey()).append(" , ").append(message.getValue()).append(" ]");
        }
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

}
