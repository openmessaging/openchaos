package io.openchaos.common.utils;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SshUtilTest {

    @Test
    public void init() throws Exception {
        File file = new File("src/test/Resources/Nodes.yaml");
        String user = "root";
        String password = "Yigeyy00";
        List<String> nodes = new ArrayList<>();
        if (file.isFile() && file.exists())
        { // 判断文件是否存在
            InputStreamReader read = new InputStreamReader(
                    Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);// 考虑到编码格式
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt = null;

            while ((lineTxt = bufferedReader.readLine()) != null)
            {
                nodes.add(lineTxt);
            }
            bufferedReader.close();
            read.close();
        }
        else
        {
            System.out.println("找不到指定的文件");
        }
        SshUtil.init(user, password, nodes);
        for (String node : nodes) {
            assertNotNull(SshUtil.execCommandWithArgsReturnStr(node, "ls / ", ""));
        }
    }
}