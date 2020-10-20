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
import io.openmessaging.chaos.checker.result.EndToEndLatencyResult;
import io.openmessaging.chaos.checker.result.TestResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndToEndLatencyChecker implements Checker {
    public long e2eIn1msLatencyCount = 0;
    public long e2eIn5msLatencyCount = 0;
    public long e2eIn10msLatencyCount = 0;
    public long e2eIn100msLatencyCount = 0;
    public long e2eIn1000msLatencyCount = 0;
    public long e2eIn3000msLatencyCount = 0;
    public long e2eExceed3000msLatencyCount = 0;

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(MQChecker.class);

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private String fileName;
    private String outputDir;
    private String originFilePath;
    private String filePath;

    public EndToEndLatencyChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }

    @Override public TestResult check() {
        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history", "endToEndLatency-result");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history", "endToEndLatency-result");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        EndToEndLatencyResult endToEndLatency = null;

        try {
            checkInner();
            endToEndLatency = generateResult();
            MAPPER.writeValue(new File(filePath), endToEndLatency);
        } catch (Exception e) {
            log.error("MQChecker check fail", e);
        }

        return endToEndLatency;
    }

    private void checkInner() throws IOException {
        Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
            filter(x -> x[2].equals("RESPONSE") && x[1].equals("dequeue")).filter(x -> Long.parseLong(x[9]) >= 0).
            map(x -> x[9]).forEach(x -> {
                long latency = Long.parseLong(x.trim());
                if (latency <= 1) {
                    e2eIn1msLatencyCount++;
                } else if (latency <= 5) {
                    e2eIn5msLatencyCount++;
                } else if (latency <= 10) {
                    e2eIn10msLatencyCount++;
                } else if (latency <= 100) {
                    e2eIn100msLatencyCount++;
                } else if (latency <= 1000) {
                    e2eIn1000msLatencyCount++;
                } else if (latency <= 3000) {
                    e2eIn3000msLatencyCount++;
                } else {
                    e2eExceed3000msLatencyCount++;
                }
            });
    }

    private EndToEndLatencyResult generateResult() {
        EndToEndLatencyResult endToEndLatencyResult = new EndToEndLatencyResult();
        endToEndLatencyResult.e2eIn1msLatencyCount = e2eIn1msLatencyCount;
        endToEndLatencyResult.e2eIn5msLatencyCount = e2eIn5msLatencyCount;
        endToEndLatencyResult.e2eIn10msLatencyCount = e2eIn10msLatencyCount;
        endToEndLatencyResult.e2eIn100msLatencyCount = e2eIn100msLatencyCount;
        endToEndLatencyResult.e2eIn1000msLatencyCount = e2eIn1000msLatencyCount;
        endToEndLatencyResult.e2eIn3000msLatencyCount = e2eIn3000msLatencyCount;
        endToEndLatencyResult.e2eExceed3000msLatencyCount = e2eExceed3000msLatencyCount;
        endToEndLatencyResult.isValid = true;
        return endToEndLatencyResult;
    }
}
