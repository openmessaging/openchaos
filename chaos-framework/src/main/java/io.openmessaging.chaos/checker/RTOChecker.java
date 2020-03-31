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
import io.openmessaging.chaos.checker.result.RTORecord;
import io.openmessaging.chaos.checker.result.RTOTestResult;
import io.openmessaging.chaos.checker.result.TestResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTOChecker implements Checker {

    private String fileName;
    private static final Logger logger = LoggerFactory.getLogger(RTOChecker.class);
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    static {
        mapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    public RTOChecker(String fileName) {
        this.fileName = fileName;
    }

    @Override public TestResult check() {
        RTOTestResult rtoTestResult = new RTOTestResult();
        try {
            generateRTOTestResult(rtoTestResult);
        } catch (Exception e) {
            logger.error("", e);
            rtoTestResult.isValid = false;
        }
        try{
            mapper.writeValue(new File(fileName.replace("history", "rto-result")), rtoTestResult);
        } catch (Exception e){
            logger.error("", e);
        }

        return rtoTestResult;
    }

    private void generateRTOTestResult(RTOTestResult rtoTestResult) throws Exception {

        List<String[]> allRecords = Files.lines(Paths.get(fileName)).map(x -> x.split("\t")).filter(x -> x[0].equals("fault") || (x[1].equals("enqueue") && x[2].equals("RESPONSE"))).collect(Collectors.toList());
        boolean isInFault = false;
        boolean unavailableFlag = false;
        RTORecord rtoRecord = null;

        for (String[] x : allRecords) {
            if (x[0].equals("fault") && x[2].equals("start")) {
                isInFault = true;
                rtoRecord = new RTORecord();
                rtoTestResult.getResults().add(rtoRecord);
            }

            if (isInFault && !unavailableFlag && x[3].equals("FAILURE")) {
                rtoRecord.isUnavailable = true;
                rtoRecord.startTimestamp = Long.parseLong(x[6]) - Long.parseLong(x[7]);
                unavailableFlag = true;
            }

            if (isInFault && unavailableFlag && x[3].equals("SUCCESS")) {
                rtoRecord.endTimestamp = Long.parseLong(x[6]);
                rtoRecord.RTOTime = rtoRecord.endTimestamp - rtoRecord.startTimestamp;
                rtoRecord.isRecoveryInFaultInterval = true;
                unavailableFlag = false;
            }
            
            if (isInFault && x[0].equals("fault") && x[2].equals("end")) {
                isInFault = false;
            }
        }

        rtoTestResult.isValid=true;

    }

    public static void main(String[] args) {
        RTOChecker rtoChecker = new RTOChecker("2020-03-22-16-23-34-RocketMQ-chaos-history-file");
        System.out.println(rtoChecker.check());
    }
}
