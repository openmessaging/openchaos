package io.openchaos.driver.nacos;

import io.openchaos.driver.ChaosDriver;
import java.util.List;

public interface NacosDriver extends ChaosDriver {
    /**
     * create chaos client
     * @return
     */
    NacosClient createClient(int id);
    NacosClient createClient(int id,NacosCallback nacosCallback);
    List<String> getgroup();
    List<String> getdataIds();
    int getNUM();
}


