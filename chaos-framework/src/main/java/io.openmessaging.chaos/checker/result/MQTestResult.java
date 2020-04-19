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

package io.openmessaging.chaos.checker.result;

import com.google.common.collect.Multiset;
import java.util.Set;

public class MQTestResult extends TestResult {
    public long enqueueInvokeCount;
    public long enqueueSuccessCount;
    public long enqueueActualCount;
    public long lostMessageCount;
    public Set<String> lostMessages;
    public long duplicateMessageCount;
    public Multiset<String> duplicateMessages;
    public boolean atMostOnce;
    public boolean atLeastOnce;
    public boolean exactlyOnce;

    public MQTestResult() {
        super("MQTestResult");
    }

    @Override
    public String toString() {
        return "\n" + name + "{" +
            "\n\tenqueueInvokeCount=" + enqueueInvokeCount +
            "\n\tenqueueSuccessCount=" + enqueueSuccessCount +
            "\n\tenqueueActualCount=" + enqueueActualCount +
            "\n\tlostMessageCount=" + lostMessageCount +
            "\n\tlostMessages=" + lostMessages +
            "\n\tduplicateMessageCount=" + duplicateMessageCount +
            "\n\tduplicateMessages=" + duplicateMessages +
            "\n\tatMostOnce=" + atMostOnce +
            "\n\tatLeastOnce=" + atLeastOnce +
            "\n\texactlyOnce=" + exactlyOnce +
            "\n\tisValid=" + isValid +
            "\n }";
    }
}
