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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.NamedPlotColor;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.ImageTerminal;
import io.openchaos.checker.result.PerfTestResult;
import io.openchaos.checker.result.TestResult;
import io.openchaos.OssConfig;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfChecker implements Checker {

    private static final Logger log = LoggerFactory.getLogger(PerfChecker.class);
    private String outputDir;
    private String fileName;
    private long testStartTimestamp;
    private long testEndTimestamp;
    private String filePath;
    private String originFilePath;
    private List<String> points;
    private boolean isUploadImage;
    private OssConfig ossConfig;

    public PerfChecker(List<String> points, String outputDir, String fileName, long testStartTimestamp,
        long testEndTimestamp, boolean isUploadImage, OssConfig ossConfig) {
        this.points = points;
        this.outputDir = outputDir;
        this.fileName = fileName;
        this.testStartTimestamp = testStartTimestamp;
        this.testEndTimestamp = testEndTimestamp;
        this.isUploadImage = isUploadImage;
        this.ossConfig = ossConfig;
    }

    @Override
    public TestResult check() {
        if (outputDir != null && !outputDir.isEmpty()) {
            originFilePath = outputDir + File.separator + fileName;
            filePath = outputDir + File.separator + fileName.replace("history-file", "latency-point-graph.png");
        } else {
            originFilePath = fileName;
            filePath = fileName.replace("history-file", "latency-point-graph.png");
        }

        if (!new File(originFilePath).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }

        PerfTestResult testResult = new PerfTestResult();
        testResult.isValid = true;
        try {
            generateLatencyPointGraph();
        } catch (Exception e) {
            log.error("", e);
            testResult.isValid = false;
        }

        if (testResult.isValid && isUploadImage) {
            testResult.setOssUrl(uploadImage2Oss());
        }

        return testResult;
    }

    private String uploadImage2Oss() {
        OSS pluginStoreOssClient = new OSSClientBuilder().build(ossConfig.ossEndPoint, ossConfig.ossAccessKeyId,
            ossConfig.ossAccessKeySecret);
        try {
            pluginStoreOssClient.putObject(ossConfig.bucketName, filePath, new FileInputStream(new File(filePath)));
        } catch (Exception e) {
            log.error("Upload image to oss failed", e);
            return null;
        }
        String endPoint;
        if (ossConfig.ossEndPoint.startsWith("http://")) {
            endPoint = ossConfig.ossEndPoint.substring(7);
        } else {
            endPoint = ossConfig.ossEndPoint;
        }
        return "http://" + ossConfig.bucketName + "." + endPoint + "/" + filePath;
    }

    private void generateLatencyPointGraph() throws Exception {

        ImageTerminal png = new ImageTerminal();
        File file = new File(filePath);
        boolean isCreate = file.createNewFile();
        if (!isCreate)
            throw new IOException("Create file fail");
        png.processOutput(new FileInputStream(file));

        JavaPlot p = new JavaPlot();
        p.setTerminal(png);

        p.setTitle("OpenChaos Latency Point Graph");

        p.getAxis("x").setLabel("time(s)");
        p.getAxis("x").setBoundaries(0, (testEndTimestamp - testStartTimestamp) / 1000 + 20);
        p.getAxis("y").setLabel("latency(ms)");
        p.getAxis("y").setBoundaries(0.1, 10 * 1000);
        p.getAxis("y").setLogScale(true);
        p.setKey(JavaPlot.Key.TOP_RIGHT);

        List<Point> faultIntervalList = new ArrayList<>();
        List<Point> invokeSuccessList = new ArrayList<>();
        List<Point> invokeFailureList = new ArrayList<>();
        List<Point> invokeUnknownList = new ArrayList<>();

        //Fault interval
        List<String[]> faultLines = Files.lines(Paths.get(originFilePath)).
            filter(x -> x.startsWith("fault")).map(x -> x.split("\t")).collect(Collectors.toList());

        for (int i = 0; i < faultLines.size(); ) {
            if (faultLines.get(i)[2].equals("start")) {
                long startTimestamp = Long.parseLong(faultLines.get(i)[3]);
                i++;
                while (i < faultLines.size() && !faultLines.get(i)[2].equals("end")) {
                    i++;
                }
                if (i >= faultLines.size())
                    break;
                long endTimestamp = Long.parseLong(faultLines.get(i)[3]);
                long x1 = (startTimestamp - testStartTimestamp) / 1000;
                long x2 = (endTimestamp - testStartTimestamp) / 1000;
                faultIntervalList.add(new Point(x1, 0));
                faultIntervalList.add(new Point(x1, 10 * 1000));
                faultIntervalList.add(new Point(x2, 10 * 1000));
                faultIntervalList.add(new Point(x2, 0));
            } else {
                i++;
            }
        }

        if (faultIntervalList.size() != 0) {
            DataSetPlot faultSet = new DataSetPlot(pointList2Array(faultIntervalList));
            PlotStyle faultStyle = new PlotStyle();
            faultStyle.setStyle(Style.FILLEDCURVES);
            faultStyle.setLineType(NamedPlotColor.GRAY);
            faultSet.setPlotStyle(faultStyle);
            faultSet.setTitle("fault interval");
            p.addPlot(faultSet);
        }

        for (String point : points) {

            Files.lines(Paths.get(originFilePath)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).filter(x -> x[2].equals("RESPONSE")).filter(x -> Long.parseLong(x[7]) >= 0).forEach(line -> {
                if (line[1].equals(point)) {
                    switch (line[3]) {
                        case "SUCCESS":
                            invokeSuccessList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                            break;
                        case "FAILURE":
                            invokeFailureList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                            break;
                        case "UNKNOWN":
                            invokeUnknownList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                            break;
                        default:
                            log.error("Error data in invoke");
                    }
                }
            });

            if (invokeSuccessList.size() != 0) {
                renderPoint(p, invokeSuccessList, point + " success", 4, NamedPlotColor.GREEN);
            }

            if (invokeFailureList.size() != 0) {
                renderPoint(p, invokeFailureList, point + " failure", 4, NamedPlotColor.RED);
            }

            if (invokeUnknownList.size() != 0) {
                renderPoint(p, invokeUnknownList, point + " unknown", 4, NamedPlotColor.BLUE);
            }

            invokeSuccessList.clear();
            invokeFailureList.clear();
            invokeUnknownList.clear();

        }

        p.setKey(JavaPlot.Key.BELOW);

        p.plot();

        ImageIO.write(png.getImage(), "png", file);
    }

    private void renderPoint(JavaPlot plot, List<Point> dataSet, String title, int pointType, NamedPlotColor color) {
        DataSetPlot dataSetPlot = new DataSetPlot(pointList2Array(dataSet));
        PlotStyle plotStyle = new PlotStyle();
        plotStyle.setStyle(Style.POINTS);
        plotStyle.setPointType(pointType);
        plotStyle.setLineType(color);
        dataSetPlot.setPlotStyle(plotStyle);
        dataSetPlot.setTitle(title);
        plot.addPlot(dataSetPlot);
    }

    private long[][] pointList2Array(List<Point> list) {
        long[][] res = new long[list.size()][2];
        for (int i = 0; i < list.size(); i++) {
            res[i][0] = list.get(i).x;
            res[i][1] = list.get(i).y;
        }
        return res;
    }

    static class Point {
        long x;
        long y;

        public Point(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }
}
