package io.openchaos.common.utils;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SshUtilTest {
    static String user = "root";
    static String password = "Yigeyy00";
    static List<String> nodes = new ArrayList<String>(){{add("47.103.146.210");}};
    @Test
    public void init() throws Exception {
        SshUtil.init(user, password, nodes);
        for (String node : nodes) {
            assertNotNull(SshUtil.execCommandWithArgsReturnStr(node, "docker ps ", ""));
        }
    }

    public static void main(String[] args) throws Exception {
        Result result = JUnitCore.runClasses(SshUtilTest.class);
        for (Failure failure : result.getFailures()){
            System.out.println(failure.toString());
        }
        System.out.println("测试结果为: " + result.wasSuccessful());
    }
}