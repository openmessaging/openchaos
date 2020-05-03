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
import io.openmessaging.chaos.checker.result.RecoveryRecord;
import io.openmessaging.chaos.checker.result.RecoveryTestResult;
import io.openmessaging.chaos.checker.result.TestResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryChecker implements Checker {
    private static final Logger log = LoggerFactory.getLogger(RecoveryChecker.class);
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    private String fileName;

    public RecoveryChecker(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public TestResult check() {
        RecoveryTestResult recoveryTestResult = new RecoveryTestResult();
        try {
            checkInner(recoveryTestResult);
        } catch (Exception e) {
            log.error("", e);
            recoveryTestResult.isValid = false;
        }
        try {
            MAPPER.writeValue(new File(fileName.replace("history", "recovery-result")), recoveryTestResult);
        } catch (Exception e) {
            log.error("", e);
        }

        return recoveryTestResult;
    }

    private void checkInner(RecoveryTestResult recoveryTestResult) throws Exception {

        List<String[]> allRecords = Files.lines(Paths.get(fileName)).map(x -> x.split("\t")).filter(x -> x[0].equals("fault") || (x[1].equals("enqueue") && x[2].equals("RESPONSE"))).collect(Collectors.toList());

        RecoveryRecord recoveryRecord = null;

        boolean unavailableFlag = false;

        for (String[] x : allRecords) {

            if (!unavailableFlag && x[3].equals("FAILURE")) {

                RecoveryRecord lastRecord = recoveryTestResult.getResults().getLast();

                if (lastRecord != null && Long.parseLong(x[6]) - lastRecord.unavailableEndTimestamp < 50) {
                    continue;
                }

                recoveryRecord = new RecoveryRecord();
                recoveryTestResult.getResults().add(recoveryRecord);

                recoveryRecord.unavailableStartTimestamp = Long.parseLong(x[6]) - Long.parseLong(x[7]);
                unavailableFlag = true;
            }

            if (unavailableFlag && x[3].equals("SUCCESS") && recoveryRecord.unavailableEndTimestamp == 0) {

                recoveryRecord.unavailableEndTimestamp = Long.parseLong(x[6]);
                recoveryRecord.recoveryTime = recoveryRecord.unavailableEndTimestamp - recoveryRecord.unavailableStartTimestamp;
                unavailableFlag = false;
            }

        }

        recoveryTestResult.isValid = true;
    }

}
