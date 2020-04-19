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

import java.util.ArrayList;
import java.util.List;

public class RTOTestResult extends TestResult {
    private List<RTORecord> results = new ArrayList<>();

    private boolean unexpectedUnavailableInNormalInterval = false;

    public RTOTestResult() {
        super("RTOTestResult");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n" + name + "{ ");
        int i = 1;
        int count = 0;
        long sum = 0;
        boolean allRecoveryInFaultInterval = true;
        for (RTORecord record : results) {
            stringBuilder.append("\n\tfault interval ").append(i++).append(" : ").append(record);
            if (record.isUnavailableInFaultInterval && record.isRecoveryInFaultInterval) {
                sum += record.rtoTime;
                count++;
            }
            if (record.isUnavailableInFaultInterval && !record.isRecoveryInFaultInterval) {
                allRecoveryInFaultInterval = false;
            }
        }
        if (allRecoveryInFaultInterval && count != 0) {
            stringBuilder.append("\n\taverage failure recovery time = ").append(sum / count).append("ms");
        }
        if (!allRecoveryInFaultInterval) {
            stringBuilder.append("\n\tnote: the system can not be recovered during some fault periods!!!");
        }
        if (unexpectedUnavailableInNormalInterval) {
            stringBuilder.append("\n\tnote: unexpected unavailable time in normal interval!!!");
        }
        stringBuilder.append("\n\t").append("isValid=").append(isValid);
        stringBuilder.append("\n}");
        return stringBuilder.toString();
    }

    public List<RTORecord> getResults() {
        return results;
    }

    public boolean isUnexpectedUnavailableInNormalInterval() {
        return unexpectedUnavailableInNormalInterval;
    }

    public void setUnexpectedUnavailableInNormalInterval(boolean unexpectedUnavailableInNormalInterval) {
        this.unexpectedUnavailableInNormalInterval = unexpectedUnavailableInNormalInterval;
    }
}
