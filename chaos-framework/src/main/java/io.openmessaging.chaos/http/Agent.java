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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Agent extends AbstractVerticle {

    private static Vertx vertxStatic = null;

    private int port;

    private Agent(int port) {
        this.port = port;
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        router.post("/start").handler(this::handleStart);
        router.post("/stop").handler(this::handleStop);
        router.post("/record").handler(this::handleRecord);
        router.get("/status").handler(this::getStatus);
        router.get("/result").handler(this::getResult);

        vertx.createHttpServer().requestHandler(router::accept).listen(port);
    }

    private void handleStart(RoutingContext routingContext) {

    }

    private void handleStop(RoutingContext routingContext) {

    }

    private void handleRecord(RoutingContext routingContext) {

    }

    private void getStatus(RoutingContext routingContext) {

    }

    private void getResult(RoutingContext routingContext) {

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
