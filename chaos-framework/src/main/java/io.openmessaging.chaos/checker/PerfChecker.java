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

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.NamedPlotColor;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.ImageTerminal;
import io.openmessaging.chaos.checker.result.TestResult;
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
    private String fileName;
    private long testStartTimestamp;
    private long testEndTimestamp;

    public PerfChecker(String fileName, long testStartTimestamp, long testEndTimestamp) {
        this.fileName = fileName;
        this.testStartTimestamp = testStartTimestamp;
        this.testEndTimestamp = testEndTimestamp;
    }

    @Override
    public TestResult check() {
        TestResult testResult = new TestResult("PerfTestResult");
        testResult.isValid = true;
        if (!new File(fileName).exists()) {
            System.err.println("File not exist.");
            System.exit(0);
        }
        try {
            generateLatencyPointGraph();
        } catch (Exception e) {
            log.error("", e);
            testResult.isValid = false;
        }

        return testResult;
    }

    private void generateLatencyPointGraph() throws Exception {

        ImageTerminal png = new ImageTerminal();
        File file = new File(fileName.replace("history-file", "latency-point-graph.png"));
        boolean isCreate = file.createNewFile();
        if (!isCreate)
            throw new IOException("Create file fail");
        png.processOutput(new FileInputStream(file));

        JavaPlot p = new JavaPlot();
        p.setTerminal(png);

        p.setTitle("OpenMessaging-Chaos Latency Point Graph");

        p.getAxis("x").setLabel("time(s)");
        p.getAxis("x").setBoundaries(0, (testEndTimestamp - testStartTimestamp) / 1000 + 20);
        p.getAxis("y").setLabel("latency(ms)");
        p.getAxis("y").setBoundaries(0.1, 10 * 1000);
        p.getAxis("y").setLogScale(true);
        p.setKey(JavaPlot.Key.TOP_RIGHT);

        List<Point> faultIntervalList = new ArrayList<>();
        List<Point> enqueueSuccessList = new ArrayList<>();
        List<Point> enqueueFailureList = new ArrayList<>();
        List<Point> enqueueUnknownList = new ArrayList<>();

        //Fault interval
        List<String[]> faultLines = Files.lines(Paths.get(fileName)).
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

        Files.lines(Paths.get(fileName)).map(x -> x.split("\t")).filter(x -> !x[0].equals("fault")).filter(x -> x[2].equals("RESPONSE")).filter(x -> Long.parseLong(x[7]) >= 0).forEach(line -> {
            if (line[1].equals("enqueue")) {
                switch (line[3]) {
                    case "SUCCESS":
                        enqueueSuccessList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                        break;
                    case "FAILURE":
                        enqueueFailureList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                        break;
                    case "UNKNOWN":
                        enqueueUnknownList.add(new Point((Long.parseLong(line[6]) - testStartTimestamp) / 1000, Long.parseLong(line[7])));
                        break;
                    default:
                        log.error("Error data in enqueue");
                }
            }
        });

        if (enqueueSuccessList.size() != 0) {
            renderPoint(p, enqueueSuccessList, "enqueue success", 4, NamedPlotColor.GREEN);
        }

        if (enqueueFailureList.size() != 0) {
            renderPoint(p, enqueueFailureList, "enqueue failure", 4, NamedPlotColor.RED);
        }

        if (enqueueUnknownList.size() != 0) {
            renderPoint(p, enqueueUnknownList, "enqueue unknown", 4, NamedPlotColor.BLUE);
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

    class Point {
        long x;
        long y;

        public Point(long x, long y) {
            this.x = x;
            this.y = y;
        }
    }
}
