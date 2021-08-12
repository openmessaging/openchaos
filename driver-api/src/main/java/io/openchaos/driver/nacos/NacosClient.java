package io.openchaos.driver.nacos;

import io.openchaos.common.InvokeResult;
import io.openchaos.driver.ChaosClient;

import java.util.List;
import java.util.Optional;

public interface NacosClient extends  ChaosClient {

    InvokeResult put(Optional<String> key, String dataId,String group,String config) ;

    List<String> getAll(Optional<String> key, int putInvokeCount);

    List<String> getAll(Optional<String> key);
    long getSendtimestamp();
    void start();

    void close();
}
