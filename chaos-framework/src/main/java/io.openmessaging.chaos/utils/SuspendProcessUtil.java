package io.openmessaging.chaos.utils;

import io.openmessaging.chaos.common.utils.SshUtil;
import java.util.List;

public class SuspendProcessUtil {

    public static void suspend(String node, List<String> processNames) throws Exception {
        for (String processName : processNames) {
            SshUtil.execCommand(node, String.format("ps ax | grep -i '%s' |grep java | grep -v grep | awk '{print $1}' | xargs kill -STOP", processName));
        }
    }

    public static void recover(String node, List<String> processNames) throws Exception {
        for (String processName : processNames) {
            SshUtil.execCommand(node, String.format("ps ax | grep -i '%s' |grep java | grep -v grep | awk '{print $1}' | xargs kill -CONT", processName));
        }
    }
}
