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

public class RTORecord {
    public boolean isUnavailableInFaultInterval;
    public boolean isRecoveryInFaultInterval;
    public long rtoTime;
    public long startTimestamp;
    public long endTimestamp;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ ");
        if (!isUnavailableInFaultInterval) {
            stringBuilder.append("available all the time");
        } else if (!isRecoveryInFaultInterval) {
            stringBuilder.append("no recovery during fault interval");
        } else {
            stringBuilder.append("failure recovery time = ").append(rtoTime).append("ms");
        }
        stringBuilder.append(" }");
        return stringBuilder.toString();
    }
}
