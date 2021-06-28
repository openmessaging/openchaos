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

package io.openchaos;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.util.concurrent.RateLimiter;
import io.openchaos.checker.KVChecker;
import io.openchaos.checker.QueueChecker;
import io.openchaos.checker.PerfChecker;
import io.openchaos.checker.result.TestResult;
import io.openchaos.http.Agent;
import io.openchaos.checker.Checker;
import io.openchaos.checker.EndToEndLatencyChecker;
import io.openchaos.checker.OrderChecker;
import io.openchaos.checker.RTOChecker;
import io.openchaos.checker.RecoveryChecker;
import io.openchaos.common.utils.SshUtil;
import io.openchaos.driver.ChaosNode;
import io.openchaos.fault.Fault;
import io.openchaos.fault.KillFault;
import io.openchaos.fault.NetFault;
import io.openchaos.fault.NoopFault;
import io.openchaos.fault.PauseFault;
import io.openchaos.model.KVModel;
import io.openchaos.model.Model;
import io.openchaos.model.QueueModel;
import io.openchaos.recorder.Recorder;
import io.openchaos.worker.FaultWorker;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosControl {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final Logger log = LoggerFactory.getLogger(ChaosControl.class);

    private static volatile Status status = Status.COMPLETE;

    private static List<TestResult> resultList;

    private static Fault fault;

    private static Model model;

    private static Recorder recorder;

    private static FaultWorker faultWorker;

    private static long testStartTimeStamp;

    private static long testEndTimestamp;

    private static String historyFile;

    private static List<String> shardingKeys;

    private static File driverConfigFile;

    private static DriverConfiguration driverConfiguration;

    private static volatile boolean agentMode = false;

    private static boolean isOrderTest;

    private static boolean endToEndLatencyCheck;

    private static boolean pull;

    private static long recoveryTime;

    public static boolean isUploadImage;

    public static OssConfig ossConfig = new OssConfig();

    static {
        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
    }

    public static void main(String[] args) {

        final Arguments arguments = new Arguments();
        JCommander jc = new JCommander(arguments);
        jc.setProgramName("openchaos");

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            jc.usage();
            System.exit(-1);
        }

        if (arguments.help) {
            jc.usage();
            System.exit(-1);
        }

        if (arguments.outputDir != null && !arguments.outputDir.isEmpty()) {
            if (!judgeOrCreateDir(arguments.outputDir)) {
                System.err.println("output-dir is not a standard directory name or failed to create directory");
                System.exit(-1);
            }

            if (arguments.outputDir.substring(arguments.outputDir.length() - 1).equals(File.separator)) {
                arguments.outputDir = arguments.outputDir.substring(0, arguments.outputDir.length() - 1);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                clearAfterException();
            }
        });

        try {

            driverConfigFile = new File(arguments.driver);
            driverConfiguration = MAPPER.readValue(driverConfigFile,
                DriverConfiguration.class);

            isOrderTest = driverConfiguration.isOrderTest;
            pull = driverConfiguration.pull;
            endToEndLatencyCheck = driverConfiguration.endToEndLatencyCheck;
            isUploadImage = driverConfiguration.isUploadImage;
            ossConfig.ossEndPoint = driverConfiguration.ossEndPoint;
            ossConfig.ossAccessKeyId = driverConfiguration.ossAccessKeyId;
            ossConfig.ossAccessKeySecret = driverConfiguration.ossAccessKeySecret;
            ossConfig.bucketName = driverConfiguration.bucketName;

            if (arguments.agent) {

                agentMode = true;

                log.info("Start ChaosControl HTTP Agent...");
                Agent.startAgent(arguments.port);

            } else {

                agentMode = false;

                ready(arguments);

                if (ChaosControl.status == Status.READY_FAILED) {
                    return;
                }

                run(arguments);

                Timer timer = new Timer(true);
                timer.schedule(new TimerTask() {
                    @Override public void run() {
                        ChaosControl.status = Status.STOP_ING;
                    }
                }, arguments.time * 1000);

                while (ChaosControl.status != Status.STOP_ING) {
                    Thread.sleep(10);
                }

                stop();
                check(arguments);
                showResult();
            }

        } catch (Exception e) {
            log.error("Failed to run chaos test.", e);
            clearAfterException();
        } finally {
            clear();
        }
    }

    public static void ready(Arguments arguments) {

        ChaosControl.status = Status.READY_ING;

        recoveryTime = arguments.recoveryTime;

        try {

            if (arguments.time <= 0) {
                arguments.time = Integer.MAX_VALUE;
            }

            if (arguments.concurrency <= 0) {
                throw new IllegalArgumentException("Parameter concurrency should be positive");
            }

            if (arguments.rate <= 0) {
                throw new IllegalArgumentException("Parameter rate should be positive");
            }

            List<String> faultNodeList = new ArrayList<>();

            if (!agentMode) {

                if (arguments.fault.startsWith("fixed-")) {
                    if (arguments.faultNodes == null || arguments.faultNodes.isEmpty()) {
                        throw new IllegalArgumentException("fault-nodes parameter can not be null or empty when inject fixed-xxx fault to system.");
                    } else if (driverConfiguration.nodes == null || driverConfiguration.nodes.isEmpty()) {
                        throw new IllegalArgumentException("the nodes in configure file can not be null or empty when inject fixed-xxx fault to system.");
                    } else {
                        String[] faultNodeArray = arguments.faultNodes.split(";");
                        for (String faultNode : faultNodeArray) {
                            if (!driverConfiguration.nodes.contains(faultNode)) {
                                throw new IllegalArgumentException(String.format("fault-node %s is not in current config file.", faultNode));
                            }
                        }
                        faultNodeList.addAll(Arrays.asList(faultNodeArray));
                    }
                }

            }

            List<String> allNodes = new ArrayList<>();
            if (driverConfiguration.nodes != null && !driverConfiguration.nodes.isEmpty()) {
                allNodes.addAll(driverConfiguration.nodes);
            }
            if (driverConfiguration.preNodes != null && !driverConfiguration.preNodes.isEmpty()) {
                allNodes.addAll(driverConfiguration.preNodes);
            }

            SshUtil.init(arguments.username, allNodes);

            log.info("--------------- CHAOS TEST --- DRIVER : {}---------------", driverConfiguration.name);

            historyFile = String.format("%s-%s-chaos-history-file", DATE_FORMAT.format(new Date()), driverConfiguration.name);

            RateLimiter rateLimiter = RateLimiter.create(arguments.rate);

            if (arguments.outputDir != null && !arguments.outputDir.isEmpty()) {
                recorder = Recorder.newRecorder(arguments.outputDir + File.separator + historyFile);
            } else {
                recorder = Recorder.newRecorder(historyFile);
            }

            if (recorder == null) {
                System.err.printf("Create %s failed", historyFile);
                System.exit(-1);
            }

            shardingKeys = new ArrayList<>();
            for (int i = 0; i < 2 * arguments.concurrency; i++) {
                shardingKeys.add("shardingKey" + i);
            }

            //Currently only queue model is supported
            switch (arguments.model) {
                case "queue":
                    model = new QueueModel(arguments.concurrency, rateLimiter, recorder, driverConfigFile, isOrderTest, pull, shardingKeys);
                    break;
                case "cache":
                    model = new KVModel(arguments.concurrency, rateLimiter, recorder, driverConfigFile);
                    break;
                default:
                    throw new RuntimeException("model not recognized.");
            }

            Map<String, ChaosNode> map = null;

            if (driverConfiguration.nodes != null && !driverConfiguration.nodes.isEmpty()) {
                map = model.setupCluster(driverConfiguration, arguments.install);
            }

            String metaName = model.getMetaName();
            String metaNode = model.getMetaNode();
            String stateClass = driverConfiguration.stateClass;
            model.setupClient();

            //Initial fault
            if (map == null || map.isEmpty()) {
                log.warn("Configure file does not contain nodes, use noop fault");
                fault = new NoopFault();
            } else {
                switch (arguments.fault) {
                    case "noop":
                        fault = new NoopFault();
                        break;
                    case "minor-kill":
                    case "major-kill":
                    case "random-kill":
                        fault = new KillFault(map, arguments.fault, recorder);
                        break;
                    case "leader-kill":
                        fault = new KillFault(map, stateClass, metaNode, metaName, arguments.fault, recorder);
                        break;
                    case "fixed-kill":
                        fault = new KillFault(map, arguments.fault, recorder, faultNodeList);
                        break;
                    case "random-partition":
                    case "random-delay":
                    case "random-loss":
                        fault = new NetFault(map.keySet(), arguments.fault, recorder);
                        break;
                    case "fixed-partition":
                        fault = new NetFault(map.keySet(), arguments.fault, recorder, faultNodeList);
                        break;
                    case "partition-majorities-ring":
                        if (driverConfiguration.nodes.size() <= 3)
                            throw new IllegalArgumentException("The number of nodes less than or equal to 3, unable to form partition-majorities-ring");
                        fault = new NetFault(map.keySet(), arguments.fault, recorder);
                        break;
                    case "bridge":
                        if (driverConfiguration.nodes.size() != 5)
                            throw new IllegalArgumentException("The number of nodes is not equal to 5, unable to form bridge");
                        fault = new NetFault(map.keySet(), arguments.fault, recorder);
                        break;
                    case "minor-suspend":
                    case "major-suspend":
                    case "random-suspend":
                        fault = new PauseFault(map, arguments.fault, recorder);
                        break;
                    case "leader-suspend":
                        fault = new PauseFault(map, stateClass, metaNode, metaName, arguments.fault, recorder);
                        break;
                    case "fixed-suspend":
                        fault = new PauseFault(map, arguments.fault, recorder, faultNodeList);
                        break;
                    default:
                        throw new RuntimeException("no such fault");
                }
            }

            //Ensure cluster and clients are ready
//            log.info("Probe cluster, ensure cluster and clients are ready...");
//            boolean isReady = model.probeCluster();
//            if (!isReady) {
//                throw new RuntimeException("Cluster and clients are not ready, please check.");
//            }
            log.info("Cluster and clients are ready.");

            ChaosControl.status = Status.READY_COMPLETE;

        } catch (Throwable e) {

            ChaosControl.status = Status.READY_FAILED;
            log.error("Failed to ready chaos test.", e);
            clearAfterException();
        }
    }

    public static void run(Arguments arguments) {

        ChaosControl.status = Status.RUN_ING;
        //Start fault worker
        faultWorker = new FaultWorker(log, fault, arguments.interval);

        faultWorker.start();

        testStartTimeStamp = System.currentTimeMillis();

        //Start model
        model.start();

    }

    public static void stop() {

        ChaosControl.status = Status.STOP_ING;
        //Stop model
        model.stop();

        //Interrupt fault worker
        faultWorker.breakLoop();

        //Recovery fault
        fault.recover();

        log.info("Wait for recovery some time");

        try {
            //Wait for recovery
            Thread.sleep(TimeUnit.SECONDS.toMillis(recoveryTime));
        } catch (Exception e) {
            log.info("", e);
        }

        //Model do something after stop
        model.afterStop();

        recorder.flush();

        testEndTimestamp = System.currentTimeMillis();
    }

    public static void check(Arguments arguments) {

        log.info("Start check...");
        ChaosControl.status = Status.CHECK_ING;

        List<Checker> checkerList = new ArrayList<>();

        List<String> points;

        switch (arguments.model) {
            case "queue":
                checkerList.add(new QueueChecker(arguments.outputDir, historyFile));
                points = Collections.singletonList("enqueue");
                checkerList.add(new PerfChecker(points, arguments.outputDir, historyFile, testStartTimeStamp, testEndTimestamp, isUploadImage, ossConfig));
                break;
            case "cache":
                checkerList.add(new KVChecker(arguments.outputDir, historyFile));
                points = Collections.singletonList("put");
                checkerList.add(new PerfChecker(points, arguments.outputDir, historyFile, testStartTimeStamp, testEndTimestamp, isUploadImage, ossConfig));
                break;
            default:
                break;
        }

        if (arguments.rto) {
            checkerList.add(new RTOChecker(arguments.outputDir, historyFile, arguments.model));
        }
        if (arguments.recovery) {
            checkerList.add(new RecoveryChecker(arguments.outputDir, historyFile, arguments.model));
        }
        if (isOrderTest) {
            checkerList.add(new OrderChecker(arguments.outputDir, historyFile, shardingKeys));
        }

        if (endToEndLatencyCheck) {
            checkerList.add(new EndToEndLatencyChecker(arguments.outputDir, historyFile));
        }

        resultList = new ArrayList<>();
        checkerList.forEach(checker -> resultList.add(checker.check()));

        log.info("Check complete.");

        ChaosControl.status = Status.COMPLETE;
    }

    private static void showResult() {

        if (resultList.size() != 0) {
            log.info("--------------- CHAOS TEST RESULT --- DRIVER : {}---------------", driverConfiguration.name);
            resultList.forEach(testResult -> log.info(testResult.toString()));
            log.info("--------------- CHAOS TEST RESULT --- DRIVER : {}---------------", driverConfiguration.name);
        }
    }

    private static void clearAfterException() {

        if (recorder != null) {
            recorder.close();
            recorder.delete();
            recorder = null;
        }

        if (model != null) {
            model.shutdown();
            model = null;
        }

        SshUtil.close();
    }

    public static void setStatus(Status status) {
        ChaosControl.status = status;
    }

    public static Status getStatus() {
        return status;
    }

    public static void setResultList(List<TestResult> resultList) {
        ChaosControl.resultList = resultList;
    }

    public static List<TestResult> getResultList() {
        return resultList;
    }

    public static Recorder getRecorder() {
        return recorder;
    }

    public static void clear() {

        if (recorder != null) {
            recorder.close();
            recorder = null;
        }

        if (model != null) {
            model.stop();
            model.shutdown();
            model = null;
        }

        fault = null;
        faultWorker = null;
        testStartTimeStamp = 0;
        testEndTimestamp = 0;
        historyFile = null;
        shardingKeys = null;

        SshUtil.close();
    }

    public enum Status {
        READY_ING,
        READY_COMPLETE,
        READY_FAILED,
        RUN_ING,
        STOP_ING,
        CHECK_ING,
        COMPLETE,
    }

    public static boolean judgeOrCreateDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs();
        } else if (!file.isDirectory()) {
            log.error("output-dir is not a directory");
            return false;
        } else {
            return true;
        }
    }
}
