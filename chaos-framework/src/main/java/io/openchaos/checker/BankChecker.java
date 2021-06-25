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
package io.openchaos.checker;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.checker.result.BankTestResult;
import io.openchaos.checker.result.CacheTestResult;
import io.openchaos.checker.result.TestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BankChecker implements Checker {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(BankChecker.class);
    private String outputDir;
    private String fileName;
    private String originFilePath;
    private String filePath;

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    public BankChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }

    @Override public TestResult check() {
        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history", "bank-result");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history", "bank-result");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        BankTestResult cacheTestResult = null;

        try {
            cacheTestResult = checkInner();
            MAPPER.writeValue(new File(filePath), cacheTestResult);
        } catch (Exception e) {
            log.error("CacheChecker check fail", e);
        }

        return cacheTestResult;

    }

    private BankTestResult checkInner() throws IOException {
        BankTestResult bankTestResult = new BankTestResult();
        bankTestResult.transferInvokeCount = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("transfer") && x[2].equals("REQUEST")).count();

        List<String> putSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("transfer") && x[3].equals("SUCCESS")).map(x -> x[4]).collect(Collectors.toList());

        List<String> getSuccessList = new ArrayList<>();
        List<String> getValueLines = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
            filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("getAllAmt") && x[3].equals("SUCCESS")).map(x -> x[4]).collect(Collectors.toList());
        for (String line : getValueLines) {
            getSuccessList.addAll(Arrays.stream(line.substring(1, line.length() - 1).split(",")).map(String::trim).collect(Collectors.toList()));
        }

        int totalAmt = getSuccessList.stream().map(s -> Integer.valueOf(s)).reduce(Integer::sum).orElse(0);

        bankTestResult.transferSuccessCount = putSuccessSet.size();
        bankTestResult.accountAmtTotal = totalAmt;
        return bankTestResult;
    }

}
