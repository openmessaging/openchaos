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

package io.openmessaging.chaos.checker;

import io.openmessaging.chaos.checker.result.OrderTestResult;
import io.openmessaging.chaos.checker.result.TestResult;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderChecker implements Checker {

    private static final Logger log = LoggerFactory.getLogger(OrderTestResult.class);
    private String fileName;
    private List<String> shardingKeys;

    public OrderChecker(String fileName, List<String> shardingKeys) {
        this.fileName = fileName;
        this.shardingKeys = shardingKeys;
    }

    @Override
    public TestResult check() {
        OrderTestResult orderTestResult = new OrderTestResult();
        try {
            checkInner(orderTestResult);
        } catch (Exception e) {
            log.error("", e);
            orderTestResult.isValid = false;
        }
        return orderTestResult;
    }

    public void checkInner(OrderTestResult orderTestResult) throws Exception {

        List<String[]> allRecords = Files.lines(Paths.get(fileName)).map(x -> x.split("\t")).filter(x -> x[2].equals("RESPONSE") && x[3].equals("SUCCESS")).collect(Collectors.toList());
        orderTestResult.setOrder(true);
        for (String shardingKey : shardingKeys) {
            List<String> enqueueRecords = allRecords.stream().filter(x -> x[1].equals("enqueue") && x[5].equals(shardingKey)).map(x -> x[4]).collect(Collectors.toList());
            List<String> dequeueRecords = allRecords.stream().filter(x -> x[1].equals("dequeue") && x[5].equals(shardingKey)).map(x -> x[4]).collect(Collectors.toList());
            int dequeueIndex = 0;
            int enqueueIndex = 0;
            while (enqueueIndex < enqueueRecords.size() && dequeueIndex < dequeueRecords.size()) {
                if (enqueueRecords.get(enqueueIndex).equals(dequeueRecords.get(dequeueIndex))) {
                    enqueueIndex++;
                    dequeueIndex++;
                } else {
                    dequeueIndex++;
                }
            }
            if (enqueueIndex < enqueueRecords.size()) {
                orderTestResult.setOrder(false);
                orderTestResult.setWrongShardingKey(shardingKey);
                orderTestResult.setWrongStartValue(enqueueRecords.get(enqueueIndex));
                break;
            }
        }
        orderTestResult.isValid = true;
    }

}
