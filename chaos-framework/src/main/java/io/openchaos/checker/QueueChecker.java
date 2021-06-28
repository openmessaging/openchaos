/*
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

package io.openchaos.checker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import io.openchaos.checker.result.QueueTestResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueChecker implements Checker {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(QueueChecker.class);

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private Map<String, String> lostMap = new HashMap<>();
    private Map<String, String> extraInfoMap = new HashMap<>();
    private Multiset<String> duplicateSet = HashMultiset.create();
    private AtomicLong enqueueInvokeCount = new AtomicLong();
    private AtomicLong enqueueSuccessCount = new AtomicLong();
    private AtomicLong dequeueSuccessCount = new AtomicLong();
    private String fileName;
    private String outputDir;
    private String originFilePath;
    private String filePath;

    public QueueChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }

    @Override
    public QueueTestResult check() {

        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history", "mq-result");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history", "mq-result");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        QueueTestResult queueTestResult = null;

        try {
            checkInner();
            queueTestResult = generateResult();
            MAPPER.writeValue(new File(filePath), queueTestResult);
        } catch (Exception e) {
            log.error("MQChecker check fail", e);
        }

        return queueTestResult;
    }

    private void checkInner() throws IOException {
        Files.lines(Paths.get(originFilePath)).
            map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).
            forEach(line -> {
                    if (line[1].equals("enqueue") && line[2].equals("REQUEST")) {
                        enqueueInvokeCount.incrementAndGet();
                    } else if (line[3].equals("SUCCESS")) {
                        if (line[1].equals("enqueue")) {
                            enqueueSuccessCount.incrementAndGet();
                            lostMap.put(line[4], line[8]);
                        } else if (line[1].equals("dequeue")) {
                            dequeueSuccessCount.getAndIncrement();
                            if (lostMap.containsKey(line[4])) {
                                lostMap.remove(line[4]);
                            } else {
                                duplicateSet.add(line[4]);
                                if (!extraInfoMap.containsKey(line[4])) {
                                    extraInfoMap.put(line[4], line[8]);
                                }
                            }
                        }
                    }
                });
        checkDuplicateSet();
    }

    private void checkDuplicateSet() {
        duplicateSet.removeIf(x -> {
            boolean exist = lostMap.containsKey(x);
            if (exist) {
                lostMap.remove(x);
            }
            return exist;
        });
    }

    private QueueTestResult generateResult() {
        QueueTestResult mQTestResult = new QueueTestResult();
        mQTestResult.enqueueInvokeCount = enqueueInvokeCount.get();
        mQTestResult.enqueueSuccessCount = enqueueSuccessCount.get();
        mQTestResult.dequeueSuccessCount = dequeueSuccessCount.get();
        mQTestResult.lostMessageCount = lostMap.size();
        mQTestResult.lostMessages = lostMap;
        mQTestResult.duplicateMessageCount = duplicateSet.size();
        mQTestResult.duplicateMessages = generateDuplicateMessagesMap();
        mQTestResult.atMostOnce = duplicateSet.isEmpty();
        mQTestResult.atLeastOnce = lostMap.isEmpty();
        mQTestResult.exactlyOnce = lostMap.isEmpty() && duplicateSet.isEmpty();
        mQTestResult.isValid = true;
        return mQTestResult;
    }

    private Map<String, String> generateDuplicateMessagesMap() {
        Map<String, String> res = new HashMap<>();
        for (Multiset.Entry<String> message : duplicateSet.entrySet()) {
            res.put(message.getElement(), "duplicate count = " + message.getCount() +
                ", info = " + extraInfoMap.get(message.getElement()));
        }
        return res;
    }

    public static void main(String[] args) {

    }
}
