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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import io.openmessaging.chaos.checker.result.MQTestResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQChecker implements Checker {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(MQChecker.class);

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private Set<String> lostSet = new HashSet<>();
    private Multiset<String> duplicateSet = HashMultiset.create();
    private AtomicLong enqueueInvokeCount = new AtomicLong();
    private AtomicLong enqueueSuccessCount = new AtomicLong();
    private AtomicLong dequeueSuccessCount = new AtomicLong();
    private String fileName;

    public MQChecker(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public MQTestResult check() {
        if (!new File(fileName).exists()) {
            System.out.println("File not exist.");
            System.exit(0);
        }

        MQTestResult MQTestResult = null;

        try {
            checkInner();
            MQTestResult = generateResult();
            MAPPER.writeValue(new File(fileName.replace("history", "result")), MQTestResult);
        } catch (Exception e) {
            log.error("MQChecker check fail", e);
        }

        return MQTestResult;
    }

    private void checkInner() throws IOException {
        Files.lines(Paths.get(fileName)).
            map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).
            forEach(line -> {
                if (line[1].equals("enqueue") && line[2].equals("request")) {
                    enqueueInvokeCount.incrementAndGet();
                } else if (line[3].equals("SUCCESS")) {
                    if (line[1].equals("enqueue")) {
                        enqueueSuccessCount.incrementAndGet();
                        lostSet.add(line[4]);
                    } else if (line[1].equals("dequeue")) {
                        dequeueSuccessCount.getAndIncrement();
                        if (lostSet.contains(line[4])) {
                            lostSet.remove(line[4]);
                        } else {
                            duplicateSet.add(line[4]);
                        }
                    }
                }
            });
        checkDuplicateSet(lostSet, duplicateSet);
    }

    private void checkDuplicateSet(Set<String> lostSet, Multiset<String> duplicateSet) {
        duplicateSet.removeIf(x -> {
            boolean exist = lostSet.contains(x);
            if (exist) {
                lostSet.remove(x);
            }
            return exist;
        });
    }

    private MQTestResult generateResult() {
        MQTestResult MQTestResult = new MQTestResult();
        MQTestResult.enqueueInvokeCount = enqueueInvokeCount.get();
        MQTestResult.enqueueSuccessCount = enqueueSuccessCount.get();
        MQTestResult.enqueueActualCount = dequeueSuccessCount.get();
        MQTestResult.lostMessageCount = lostSet.size();
        MQTestResult.lostMessages = lostSet;
        MQTestResult.duplicateMessageCount = duplicateSet.size();
        MQTestResult.duplicateMessages = duplicateSet;
        MQTestResult.atMostOnce = duplicateSet.isEmpty();
        MQTestResult.atLeastOnce = lostSet.isEmpty();
        MQTestResult.exactlyOnce = lostSet.isEmpty() && duplicateSet.isEmpty();
        MQTestResult.isValid = lostSet.isEmpty();
        return MQTestResult;
    }
}
