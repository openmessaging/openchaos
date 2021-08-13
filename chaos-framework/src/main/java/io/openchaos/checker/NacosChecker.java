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
import io.openchaos.checker.result.NacosEndToEndLatencyResult;
import io.openchaos.checker.result.NacosTestResult;
import io.openchaos.checker.result.TestResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

public class NacosChecker implements Checker {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger log = LoggerFactory.getLogger(KVChecker.class);
    private String outputDir;
    private String fileName;
    private String originFilePath;
    private String filePath;
    private String latencyFilePath;
    private File driverConfigFile;
    private Set<String> dataIds;
    private String group;
    private int threshold;

    private Info info = new Info();



    public NacosChecker(String outputDir, String fileName) {
        this.outputDir = outputDir;
        this.fileName = fileName;
    }
    public NacosChecker(String outputDir, String fileName,File driverConfigFile) {
        this.outputDir = outputDir;
        this.fileName = fileName;
        this.driverConfigFile = driverConfigFile;
        try {
            this.info = MAPPER.readValue(driverConfigFile, Info.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.threshold = this.info.threshold;

    }
    public TestResult check() {
        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history", "nacos-result");
            latencyFilePath = outputDir + File.separator + fileName.replace("history", "nacos-latency");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history", "nacos-result");
            latencyFilePath = fileName.replace("history", "nacos-latency");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        NacosTestResult result = null;

        try {
            result = checkInner();
            missCheckInner(result);
            orderCheckInner(result);
            MAPPER.writeValue(new File(filePath), result);
        } catch (Exception e) {
            log.error("Nacos check fail", e);
        }

        NacosEndToEndLatencyResult latenctResult = null;
        try {
            latenctResult = latencyCheckInner(result);
            MAPPER.writeValue(new File(latencyFilePath), latenctResult);
        } catch (Exception e) {
            log.error("NacosEndToEndLatency check fail", e);
        }

        return result;
    }

    private NacosTestResult checkInner() throws IOException {
        NacosTestResult result = new NacosTestResult();
        result.putInvokeCount = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("pub") && x[2].equals("REQUEST")).count();
        Set<String> putSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("pub") && x[3].equals("SUCCESS")).map(x -> x[5] + " " + x[7] + " " + x[9] + " " + x[10]).collect(Collectors.toSet());
        Set<String> getSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && x[3].equals("SUCCESS")).map(x ->x[5] + " " + x[7] + " " + x[9] + " " + x[11]).collect(Collectors.toSet());
        Set<String> subTimeOutSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && x[3].equals("TimeOut")).map(x ->x[5] + " " + x[7] + " " + x[9] + " " + x[11]).collect(Collectors.toSet());

        result.pubSuccessCount = putSuccessSet.size();
        result.subSuccessCount = getSuccessSet.size();
        putSuccessSet.removeAll(getSuccessSet);
        putSuccessSet.removeAll(subTimeOutSet);
        result.lostValues = putSuccessSet;
        result.lostValueCount = putSuccessSet.size();
        result.subTimeOutCount = subTimeOutSet.size();
        result.subTimeOutValues = subTimeOutSet;
        result.isValid = true;
        return result;
    }
    private NacosEndToEndLatencyResult latencyCheckInner(NacosTestResult resultTest) throws IOException {
        NacosEndToEndLatencyResult result = new NacosEndToEndLatencyResult();
        List<Integer> latency = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && (x[3].equals("SUCCESS") || x[3].equals("TimeOut"))).map(x -> Integer.parseInt(x[12])).collect(Collectors.toList());
        Collections.sort(latency);
        int e2eTotalLatency = latency.stream().reduce(0,Integer::sum);
        result.e2eTotalLatency = String.valueOf(e2eTotalLatency);
        result.e2eAveLatency = String.valueOf(e2eTotalLatency / latency.size());
        result.e2e50Latency = String.valueOf(percentile(latency,0.5));
        result.e2e75Latency = String.valueOf(percentile(latency,0.75));
        result.e2e85Latency = String.valueOf(percentile(latency,0.85));
        result.e2e90Latency = String.valueOf(percentile(latency,0.90));
        result.e2e95Latency = String.valueOf(percentile(latency,0.95));
        result.minLatency = String.valueOf(latency.get(0));
        result.maxLatency = String.valueOf(latency.get(latency.size() - 1));
        result.timeOutCount = String.valueOf(resultTest.subTimeOutCount);
        return result;
    }

    public static double percentile(List<Integer> data,double p) {
        int n = data.size();
//        Collections.sort(data);
        double px =  p * (n - 1);
        int i = (int) Math.floor(px);
        double g = px - i;
        if (g == 0) {
            return data.get(i);
        } else {
            return (1 - g) * data.get(i) + g * data.get(i + 1);
        }
    }
    private NacosTestResult missCheckInner(NacosTestResult result) throws IOException {

        dataIds = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive")).map(x ->x[5]).collect(Collectors.toSet());
        group = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive")).map(x ->x[7]).collect(Collectors.toList()).get(0);

        for (String dataId:dataIds) {
            Set<String> putSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                    filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("pub") && x[3].equals("SUCCESS") && x[5].equals(dataId) && x[7].equals(group)).map(x -> x[9]).collect(Collectors.toSet());
            Set<String> getSuccessSet = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                    filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && (x[3].equals("SUCCESS") || x[3].equals("TimeOut")) && x[5].equals(dataId) && x[7].equals(group)).map(x -> x[9]).collect(Collectors.toSet());
            putSuccessSet.removeAll(getSuccessSet);
            List<Long> subConfigTime = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                    filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && x[5].equals(dataId) && x[7].equals(group)).map(x ->Long.parseLong(x[10])).collect(Collectors.toList());
            int subIndex = 0;
            Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
                    filter(x -> putSuccessSet.contains(x[9])).forEach(x -> {
                        long time = Long.parseLong(x[10]);
                        while (time < subConfigTime.get(subIndex) && subConfigTime.size() > 0) {
                            subConfigTime.remove(subIndex);
                        }
                        if (subConfigTime.isEmpty() || time - subConfigTime.get(subIndex) > threshold) {
                            result.missValueCount++;
                            result.missValues.add(x[5] + " " + x[7] + " " + x[9] + " " + x[10]);
                        }
                    });
        }
        return result;
    }

    private   NacosTestResult orderCheckInner(NacosTestResult result) throws Exception {
        result.unOrderValues = new HashMap<>();
        List<String[]> allPubRecords = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("pub") && x[3].equals("SUCCESS")).collect(Collectors.toList());
        List<String[]> allSubRecords = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && (x[3].equals("SUCCESS"))).collect(Collectors.toList());
        for (String dataId:dataIds) {
            List<String> subList = allSubRecords.stream().filter(x->x[5].equals(dataId) && x[7].equals(group)).map(x->x[9]).collect(Collectors.toList());
            Map<String,Long> subMap = new HashMap<>();
            allSubRecords.stream().filter(x->x[5].equals(dataId) && x[7].equals(group)).forEach(x-> {
                subMap.put(x[9],Long.parseLong(x[10]));
            });
            List<String> pubList = allPubRecords.stream().filter(x->x[5].equals(dataId) && x[7].equals(group) && subMap.containsKey(x[9])).map(x->x[9]).collect(Collectors.toList());
            int pubIndex = 0, subIndex = 0;
            while (pubIndex < pubList.size() && subIndex < subList.size()) {
                if (!pubList.get(pubIndex).equals(subList.get(subIndex))) {
                    result.unOrderValues.put(dataId + " " + group + " " + pubList.get(pubIndex),subMap.get(pubList.get(pubIndex)));
                }
                pubIndex++;
                subIndex++;
            }

        }
        result.unOrderCount = result.unOrderValues.size();
        return  result;
    }


    static class Info {
        public int threshold;
    }
}
