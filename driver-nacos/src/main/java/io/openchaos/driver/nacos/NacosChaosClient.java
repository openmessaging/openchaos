package io.openchaos.driver.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.openchaos.common.InvokeResult;
import io.openchaos.common.NacosMessage;
import io.openchaos.driver.nacos.NacosClient;
import io.openchaos.driver.nacos.config.NacosConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executor;

public class NacosChaosClient implements NacosClient {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final Logger LOG = LoggerFactory.getLogger(NacosClient.class);

    public ConfigService NacosConfigPub;
    public ConfigService NacosConfigListener;
    public NacosConfig nacosConfig;
    public NacosCallback nacosCallback;
    public long sendtimestamp;
    private int id;
    private int NUM;
    private int threshold;
    private String dataId;
    private String group;
    private Map<String, Long> ConfigMap = new HashMap<>();


    public NacosChaosClient(NacosConfig nacosConfig ,int id) {
        this.nacosConfig = nacosConfig;
        this.id = id;
        prepare();
    }


    public NacosChaosClient(NacosConfig nacosConfig ,int id,NacosCallback nacosCallback) {
        this.nacosConfig = nacosConfig;
        this.id = id;
        this.nacosCallback = nacosCallback;
        this.threshold = nacosConfig.threshold;
        Nacosprepare();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void prepare() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.SERVER_LIST);

        properties.put("keyId", "alias/acs/mse");
        properties.put("regionId", "cn-hangzhou");
        try {
            NacosConfigPub = NacosFactory.createConfigService(properties);
            NacosConfigListener = NacosFactory.createConfigService(properties);
            String name;
            for (int i = 0; i < nacosConfig.NUM; i++) {
                name = "client: " + id + "dataId: " + nacosConfig.dataIds.get(id) + i + "group: " + nacosConfig.group.get(0);
//                boolean isSuccess = NacosConfigPub.removeConfig(nacosConfig.dataIds.get(id) + i, nacosConfig.group.get(0));
                NacosConfigListener.addListener(nacosConfig.dataIds.get(id) + i, nacosConfig.group.get(0), new DefaultConfigListener(name));
            }
        }catch (NacosException e){
            System.out.println("Nacos configservice create or listen error");
        }
    }
    public void Nacosprepare(){
        //add record listener
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, nacosConfig.SERVER_LIST);
        properties.put("keyId", "alias/acs/mse");
        properties.put("regionId", "cn-hangzhou");
        try {
            NacosConfigPub = NacosFactory.createConfigService(properties);
            NacosConfigListener = NacosFactory.createConfigService(properties);
            for (int i = 0; i < nacosConfig.NUM; i++) {
                String tempdataId = nacosConfig.dataIds.get(id) + i;
                String tempgroup = nacosConfig.group.get(0);


                NacosConfigListener.addListener(tempdataId, tempgroup, new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return null;
                    }

                    @Override
                    public void receiveConfigInfo(String s) {
                        NacosMessage message = new NacosMessage(id,tempdataId,tempgroup,s,System.currentTimeMillis());
                        if(ConfigMap.containsKey(s)){
                            message.setPubTimestamp(ConfigMap.get(s),InvokeResult.SUCCESS);
                            if(message.subTimestamp-message.pubTimestamp>threshold){
                                message.setResult(InvokeResult.TimeOut);
                            }
                            ConfigMap.remove(s);
                        }else{
                            message.setPubTimestamp(-1,InvokeResult.FAILURE);
                        }
                        nacosCallback.messageReceived(message);
                        LOG.info(id +" receive content: " + s);
                        System.out.println(id +" receive content: " + s);
                    }
                });
            }
        }catch (NacosException e){
            System.out.println("Nacos configservice create or listen error");
        }
    }

    @Override
    public InvokeResult put(Optional<String> key, String dataId,String group,String config) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        boolean result = false;

        try {
            result = NacosConfigPub.publishConfig(dataId, group, config);
            sendtimestamp = System.currentTimeMillis();
            if(result){
                ConfigMap.put(config,sendtimestamp);
                LOG.info("nacos config publish SUCCESS");
                return InvokeResult.SUCCESS;
            }else{
                LOG.warn("nacos config publish error");
                return InvokeResult.FAILURE;
            }
        }catch (NacosException e){
            LOG.info("CONFIG PUBLISH ERROR");
        }
        return InvokeResult.SUCCESS;
    }

    public long getSendtimestamp() {
        return sendtimestamp;
    }

    @Override
    public List<String> getAll(Optional<String> key, int putInvokeCount) {
        return null;
    }

    @Override
    public List<String> getAll(Optional<String> key) {
        return null;
    }

    @Override
    public void start() {

    }

    @Override
    public void close() {

    }
}
