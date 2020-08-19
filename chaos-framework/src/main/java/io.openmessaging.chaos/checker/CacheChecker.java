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
import java.util.Arrays;
import java.util.List;
import io.openmessaging.chaos.checker.result.CacheTestResult;
import io.openmessaging.chaos.checker.result.TestResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheChecker implements Checker {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(CacheChecker.class);
    private String outputDir;
    private String fileName;
    private String originFilePath;
    private String filePath;

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    public CacheChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }

    @Override public TestResult check() {
        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history", "cache-result");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history", "cache-result");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        CacheTestResult cacheTestResult = null;

        try {
            cacheTestResult = checkInner();
            MAPPER.writeValue(new File(filePath), cacheTestResult);
        } catch (Exception e) {
            log.error("MQChecker check fail", e);
        }

        return cacheTestResult;

    }

    private CacheTestResult checkInner() throws IOException {
        CacheTestResult cacheTestResult = new CacheTestResult();
        cacheTestResult.putInvokeCount = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("put") && x[2].equals("REQUEST")).count();
        Set<String> putSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("put") && x[3].equals("SUCCESS")).map(x -> x[4]).collect(Collectors.toSet());
        Set<String> getSuccessSet = new HashSet<>();
        List<String> getValueLines = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("getAll") && x[3].equals("SUCCESS")).map(x -> x[4]).collect(Collectors.toList());
        for (String line : getValueLines) {
            getSuccessSet.addAll(Arrays.stream(line.substring(1, line.length() - 1).split(",")).map(String::trim).collect(Collectors.toSet()));
        }

        cacheTestResult.putSuccessCount = putSuccessSet.size();
        cacheTestResult.getSuccessCount = getSuccessSet.size();
        putSuccessSet.removeAll(getSuccessSet);
        cacheTestResult.lostValues = putSuccessSet;
        cacheTestResult.lostValueCount = putSuccessSet.size();
        return cacheTestResult;
    }

}
