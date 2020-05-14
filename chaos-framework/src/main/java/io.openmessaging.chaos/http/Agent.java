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

package io.openmessaging.chaos.http;

import com.alibaba.fastjson.JSON;
import io.openmessaging.chaos.Arguments;
import io.openmessaging.chaos.ChaosControl;
import io.openmessaging.chaos.recorder.FaultLogEntry;
import io.openmessaging.chaos.recorder.Recorder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Agent extends AbstractVerticle {

    private static Vertx vertxStatic = null;

    private int port;

    private Agent(int port) {
        this.port = port;
    }

    private volatile Arguments arguments;

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.post("/ready").handler(this::handleReady);
        router.post("/start").handler(this::handleStart);
        router.post("/stop").handler(this::handleStop);
        router.post("/reset").handler(this::handleReset);
        router.post("/record").handler(this::handleRecord);
        router.get("/status").handler(this::getStatus);
        router.get("/result").handler(this::getResult);

        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }

    private void handleReady(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        if (ChaosControl.getStatus() == ChaosControl.Status.COMPLETE || ChaosControl.getStatus() == ChaosControl.Status.READY_FAILED) {
            arguments = JSON.parseObject(routingContext.getBodyAsString(), Arguments.class);
            new Thread(() -> ChaosControl.ready(arguments)).start();
            response.putHeader("content-type", "application/json").end("READY_ING");
        } else {
            response.putHeader("content-type", "application/json").end("FAIL");
        }
    }

    private void handleStart(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        if (ChaosControl.getStatus() == ChaosControl.Status.READY_COMPLETE) {
            new Thread(() -> ChaosControl.run(arguments)).start();
            response.putHeader("content-type", "application/json").end("START_ING");
        } else {
            response.putHeader("content-type", "application/json").end("FAIL");
        }
    }

    private void handleStop(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        if (ChaosControl.getStatus() == ChaosControl.Status.RUN_ING) {
            new Thread(() -> {
                ChaosControl.stop();
                ChaosControl.check(arguments);
                ChaosControl.clear();
            }).start();

            response.putHeader("content-type", "application/json").end("STOP_ING");
        } else {
            response.putHeader("content-type", "application/json").end("FAIL");
        }
    }

    private void handleReset(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        ChaosControl.clear();
        arguments = null;
        ChaosControl.setResultList(null);
        ChaosControl.setStatus(ChaosControl.Status.COMPLETE);
        response.putHeader("content-type", "application/json").end("OK");
    }

    private void handleRecord(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        if (ChaosControl.getStatus() == ChaosControl.Status.RUN_ING) {
            FaultLogEntry faultLogEntry = JSON.parseObject(routingContext.getBodyAsString(), FaultLogEntry.class);
            if (faultLogEntry == null) {
                response.putHeader("content-type", "application/json").end("FAIL");
                return;
            } else if (!faultLogEntry.operation.equals("start") && !faultLogEntry.operation.equals("end")) {
                response.putHeader("content-type", "application/json").end("FAIL");
                return;
            }
            Recorder recorder = ChaosControl.getRecorder();
            if (recorder != null) {
                recorder.recordFault(faultLogEntry);
                response.putHeader("content-type", "application/json").end("OK");
            } else {
                response.putHeader("content-type", "application/json").end("FAIL");
            }
        } else {
            response.putHeader("content-type", "application/json").end("FAIL");
        }
    }

    private void getStatus(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(JSON.toJSONString(ChaosControl.getStatus()));
    }

    private void getResult(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        response.putHeader("content-type", "application/json").end(JSON.toJSONString(ChaosControl.getResultList()));
    }

    public synchronized static void startAgent(int port) {
        if (vertxStatic != null) {
            return;
        }
        vertxStatic = Vertx.vertx();
        Agent agent = new Agent(port);
        vertxStatic.deployVerticle(agent);
    }

    public static void stopAgent() {
        if (vertxStatic != null) {
            vertxStatic.close();
            vertxStatic = null;
        }
    }
}
