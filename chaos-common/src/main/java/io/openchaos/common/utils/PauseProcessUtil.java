/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openchaos.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PauseProcessUtil {

    private static final Logger log = LoggerFactory.getLogger(PauseProcessUtil.class);

    public static void suspend(String node, String processName) throws Exception {
        log.info("Suspend node {} process {} .", node, processName);
        String pidList = SshUtil.execCommandWithArgsReturnStr(node, String.format("ps ax | grep -i '%s' | grep -v grep | awk '{print $1}'", processName)).trim();
        if (!pidList.isEmpty()) {
            String[] pids = pidList.split("\n");
            for (String pid : pids) {
                SshUtil.execCommand(node, String.format("kill -STOP %s", pid));
            }
        } else {
            log.info("No {} process running in node {}.", processName, node);
        }
    }

    public static void resume(String node, String processName) throws Exception {

        log.info("Resume node {} process {} .", node, processName);
        String pidList = SshUtil.execCommandWithArgsReturnStr(node, String.format("ps ax | grep -i '%s' | grep java | grep -v grep | awk '{print $1}'", processName)).trim();
        if (!pidList.isEmpty()) {
            String[] pids = pidList.split("\n");
            for (String pid : pids) {
                SshUtil.execCommand(node, String.format("kill -CONT %s", pid));
            }
        } else {
            log.info("No {} process running in node {}.", processName, node);
        }

    }
}
