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

public class OrderTestResult extends TestResult {

    private boolean isOrder;
    private String wrongShardingKey;
    private String wrongStartValue;

    public OrderTestResult() {
        super("OrderTestResult");
    }

    public boolean isOrder() {
        return isOrder;
    }

    public void setOrder(boolean order) {
        isOrder = order;
    }

    public String getWrongShardingKey() {
        return wrongShardingKey;
    }

    public void setWrongShardingKey(String wrongShardingKey) {
        this.wrongShardingKey = wrongShardingKey;
    }

    public String getWrongStartValue() {
        return wrongStartValue;
    }

    public void setWrongStartValue(String wrongStartValue) {
        this.wrongStartValue = wrongStartValue;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n" + name + "{ ");
        stringBuilder.append("\n\tpartition order = ").append(isOrder);
        if (!isOrder) {
            stringBuilder.append("\n\twrong sharding key = ").append(wrongShardingKey);
            stringBuilder.append("\n\twrong start value = ").append(wrongStartValue);
        }
        stringBuilder.append("\n\t").append("isValid=").append(isValid);
        stringBuilder.append("\n}");
        return stringBuilder.toString();
    }
}
