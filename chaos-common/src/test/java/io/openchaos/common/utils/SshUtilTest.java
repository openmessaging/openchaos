package io.openchaos.common.utils;

import org.junit.Ignore;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@Ignore("Requires external SSH host; not part of automated test suite")
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

}