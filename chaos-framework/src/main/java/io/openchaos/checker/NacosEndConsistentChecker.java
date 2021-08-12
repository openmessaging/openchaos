package io.openchaos.checker;//package io.openchaos.checker;
//
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
//import io.openchaos.checker.result.NacosEndToEndLatencyResult;
//import io.openchaos.checker.result.NacosTestResult;
//import io.openchaos.checker.result.TestResult;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//import java.util.stream.Collectors;
//
//public class NacosEndConsistentChecker implements Checker {
//    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
//            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//    private static final Logger log = LoggerFactory.getLogger(KVChecker.class);
//    private String outputDir;
//    private String fileName;
//    private String originFilePath;
//    private String filePath;
//    private String LatencyFilePath;
//    private long threshold = 15*1000;
//    static {
//        MAPPER.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
//    }
//
//    public NacosEndConsistentChecker(String outputDir, String fileName) {
//        this.outputDir = outputDir;
//        this.fileName = fileName;
//    }
//    public TestResult check(){
//        if (outputDir != null && !outputDir.isEmpty()) {
//            originFilePath = outputDir + File.separator + fileName;
//            filePath = outputDir + File.separator + fileName.replace("history", "nacos-end-result");
//            LatencyFilePath = outputDir+ File.separator + fileName.replace("history", "nacos-end-latency");
//        } else {
//            originFilePath = fileName;
//            filePath = fileName.replace("history", "nacos-end-result");
//            LatencyFilePath = fileName.replace("history", "nacos-latency");
//        }
//
//        if (!new File(originFilePath).exists()) {
//            System.err.println("File not exist.");
//            System.exit(0);
//        }
//
//        NacosTestResult result = null;
//
//        try {
//            result = checkPreInner();
//            MAPPER.writeValue(new File(filePath), result);
//        } catch (Exception e) {
//            log.error("Nacos check fail", e);
//        }
//        return result;
//    }
//
//    private NacosTestResult checkInner() throws IOException {
//        NacosTestResult result = new NacosTestResult();
//        Map<String,Long> pubSuccessMap = new HashMap<>();
//        Stack<String> pubSuccessStack = new Stack<>();
//        Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
//                filter(x -> x[1].equals("pub") && x[3].equals("SUCCESS")).forEach(x -> {
//            long timestamp = Long.parseLong(x[10]);
//            pubSuccessMap.put(x[9],timestamp);
//            pubSuccessStack.push(x[9]);
//        });
//        Map<String,Long> subSuccessMap = new HashMap<>();
//        Stack<String> subSuccessStack = new Stack<>();
//        Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
//                filter(x -> x[1].equals("receive") && x[3].equals("SUCCESS")).forEach(x -> {
//            long timestamp = Long.parseLong(x[10]);
//            subSuccessMap.put(x[9],timestamp);
//            subSuccessStack.push(x[9]);
//        });
//        while(!subSuccessStack.empty()){
//            if(subSuccessStack.peek()==pubSuccessStack.peek()){
//                subSuccessMap.remove(subSuccessStack.peek());
//                pubSuccessMap.remove(pubSuccessStack.peek());
//                subSuccessStack.pop();
//                pubSuccessStack.pop();
//            }else{
//                if(pubSuccessMap.containsKey(subSuccessStack.peek())){
//                    Long subTime = pubSuccessMap.get(subSuccessStack.peek());
//                    while(subSuccessStack.peek()!=pubSuccessStack.peek()){
//                        Long lostpubTime = pubSuccessMap.get(pubSuccessStack.peek());
//                        if(lostpubTime-subTime>threshold){
//                            result.lostValueCount ++;
//                            result.lostValues.add(subSuccessStack.peek());
//                        }
//                        subSuccessMap.remove(subSuccessStack.peek());
//                        subSuccessStack.pop();
//                    }
//                    subSuccessMap.remove(subSuccessStack.peek());
//                    pubSuccessMap.remove(pubSuccessStack.peek());
//                    subSuccessStack.pop();
//                    pubSuccessStack.pop();
//                }else{
//                    result.lostValueCount ++;
//                    result.lostValues.add(subSuccessStack.peek());
//                    subSuccessMap.remove(subSuccessStack.peek());
//                    subSuccessStack.pop();
//                }
//            }
//        }
//        return result;
//    }
//
//
//
//    private NacosTestResult checkPreInner() throws IOException {
//        NacosTestResult result = new NacosTestResult();
//        result.lostValues = new HashSet<>();
//        result.Wrong = new HashSet<>();
//        Map<String,Long> pubSuccessMap = new HashMap<>();
//        List<String> pubSuccessStack = new Stack<>();
//        Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
//                filter(x -> x[1].equals("pub") && x[3].equals("SUCCESS")).forEach(x -> {
//                    long timestamp = Long.parseLong(x[10]);
//                    pubSuccessMap.put(x[9],timestamp);
//                    pubSuccessStack.add(x[9]);
//        });
//        Map<String,Long> subSuccessMap = new HashMap<>();
//        List<String> subSuccessStack = new Stack<>();
//        Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).
//                filter(x -> x[1].equals("receive") && x[3].equals("SUCCESS")).forEach(x -> {
//            long timestamp = Long.parseLong(x[10]);
//            subSuccessMap.put(x[9],timestamp);
//            subSuccessStack.add(x[9]);
//        });
//        while(subSuccessStack.size()>0 ){
//            if(subSuccessStack.get(0)==pubSuccessStack.get(0)){
//                subSuccessMap.remove(subSuccessStack.get(0));
//                pubSuccessMap.remove(pubSuccessStack.get(0));
//                subSuccessStack.remove(0);
//                pubSuccessStack.remove(0);
//            }else{
//                if(!subSuccessMap.containsKey(pubSuccessStack.get(0)) &&
//                pubSuccessMap.containsKey(subSuccessStack.get(0))){
//                    Long subTime = subSuccessMap.get(subSuccessStack.get(0));
//                    Long pubTime = pubSuccessMap.get(pubSuccessStack.get(0));
//                    if(pubTime-subTime>threshold){
//                        result.lostValueCount++;
//                        result.lostValues.add(pubSuccessStack.get(0));
//                    }
//                    pubSuccessMap.remove(pubSuccessStack.get(0));
//                    pubSuccessStack.remove(0);
//                }else{
//                    if (!pubSuccessMap.containsKey(subSuccessStack.get(0))) {
//                        result.WrongSequence++;
//                        result.Wrong.add(subSuccessStack.get(0));
//                        subSuccessMap.remove(subSuccessStack.get(0));
//                        subSuccessStack.remove(0);
//                    }else{
//                        while(subSuccessStack.get(0)!=pubSuccessStack.get(0)){
//                            result.WrongSequence++;
//                            result.Wrong.add(subSuccessStack.get(0));
//                            subSuccessMap.remove(subSuccessStack.get(0));
//                            subSuccessStack.remove(0);
//                        }
//                        result.WrongSequence++;
//                        result.Wrong.add(subSuccessStack.get(0));
//                        subSuccessMap.remove(subSuccessStack.get(0));
//                        pubSuccessMap.remove(pubSuccessStack.get(0));
//                        subSuccessStack.remove(0);
//                        pubSuccessStack.remove(0);
//                    }
//                }
//            }
//        }
//        return result;
//    }
//    private NacosEndToEndLatencyResult latencyCheckInner() throws IOException {
//        NacosEndToEndLatencyResult result = new NacosEndToEndLatencyResult();
//        List<Integer> latency = Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).
//                filter(x -> !x[0].equals("fault")).filter(x -> x[1].equals("receive") && x[3].equals("SUCCESS")).map(x -> Integer.parseInt(x[11])).collect(Collectors.toList());
//        Collections.sort(latency);
////        int e2eTotalLatency = latency.stream().mapToInt(num -> Integer.parseInt(num)).sum();
////        int e2eTotalLatency = latency.stream().mapToInt(Integer::intValue).sum();
//        int e2eTotalLatency = latency.stream().reduce(0,Integer::sum);
//        result.e2eTotalLatency = String.valueOf(e2eTotalLatency);
//        result.e2eAveLatency = String.valueOf(e2eTotalLatency/latency.size());
//        result.e2e50Latency = String.valueOf(percentile(latency,0.5));
//        result.e2e75Latency = String.valueOf(percentile(latency,0.75));
//        result.e2e85Latency = String.valueOf(percentile(latency,0.85));
//        result.e2e90Latency = String.valueOf(percentile(latency,0.90));
//        result.e2e95Latency = String.valueOf(percentile(latency,0.95));
//        result.minLatency = String.valueOf(latency.get(0));
//        result.maxLatency = String.valueOf(latency.get(latency.size()-1));
//        return result;
//    }
//
//    public static double percentile(List<Integer> data,double p){
//        int n = data.size();
////        Collections.sort(data);
//        double px =  p*(n-1);
//        int i = (int)java.lang.Math.floor(px);
//        double g = px - i;
//        if(g==0){
//            return data.get(i);
//        }else{
//            return (1-g)*data.get(i)+g*data.get(i+1);
//        }
//    }
//}
