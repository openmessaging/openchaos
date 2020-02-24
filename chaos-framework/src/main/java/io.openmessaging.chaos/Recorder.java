/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.openmessaging.chaos;

import io.openmessaging.chaos.common.InvokeResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recode the request/response to history file
 */
public class Recorder {

    private static final Logger logger = LoggerFactory.getLogger(Recorder.class);

    private static final String OP_INVOKE = "invoke";

    private static final String OP_RETURN = "return";

    private BufferedWriter bufferedWriter;

    private File historyFile;

    private Recorder(File historyFile, BufferedWriter bufferedWriter) {
        this.historyFile = historyFile;
        this.bufferedWriter = bufferedWriter;
    }

    public void recordRequest(int clientId, String op, String data) {
        String recordLine = clientId + "\t" + op + "\t" + OP_INVOKE + "\t" + data + "\n";
        recordToHistoryFile(recordLine);

        String logLine = "client" + clientId + " invoke " + op + ", data is " + data;
        logger.info(logLine);
    }

    public void recordResponse(int clientId, String op, InvokeResult res, String data) {
        String recordLine = clientId + "\t" + op + "\t" + OP_RETURN + "\t" + res + "\t" + data + "\n";
        recordToHistoryFile(recordLine);

        String logLine = "client" + clientId + " " + op + " return " + res + ", data is " + data;
        logger.info(logLine);

    }

    private synchronized void recordToHistoryFile(String recordLine) {
        try {
            bufferedWriter.write(recordLine);
        } catch (IOException e) {
            logger.error("record to history file fail", e);
        }
    }

    public void flush() {
        try {
            bufferedWriter.flush();
        } catch (IOException e) {
            logger.error("recorder close fail", e);
        }
    }

    public void close() {
        try {
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            logger.error("recorder close fail", e);
        }
    }

    public void delete() {
        try {
            bufferedWriter.close();
            if (historyFile.exists()) {
                historyFile.delete();
            }
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    public static Recorder newRecorder(String historyFileName) {

        File historyFile = new File(historyFileName);
        if (historyFile.exists()) {
            logger.error("{} file already exist.", historyFileName);
            return null;
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(historyFileName));
        } catch (IOException e) {
            logger.error("new {} writer failed", historyFileName, e);
        }

        if (bufferedWriter != null) {
            return new Recorder(historyFile, bufferedWriter);
        } else {
            return null;
        }
    }
}
