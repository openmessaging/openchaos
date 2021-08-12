package io.openchaos.driver.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.openchaos.driver.ChaosNode;
import io.openchaos.driver.MetaNode;
import io.openchaos.driver.kv.KVClient;
import io.openchaos.driver.nacos.config.NacosConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NacosChaosDriver implements NacosDriver {

    private static final Logger log = LoggerFactory.getLogger(NacosChaosDriver.class);
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private List<String> nodes;

    private NacosConfig nacosConfig;


    @Override public NacosClient createClient(int id) {
        return new NacosChaosClient(nacosConfig,id);
    }
    @Override public NacosClient createClient(int id,NacosCallback nacosCallback) {
        return new NacosChaosClient(nacosConfig,id,nacosCallback);
    }

    @Override
    public String getMetaNode() {
        return null;
    }

    @Override
    public String getMetaName() {
        return null;
    }

    @Override public void initialize(File configurationFile, List<String> nodes) throws IOException {
        this.nodes = nodes;
        this.nacosConfig = MAPPER.readValue(configurationFile, NacosConfig.class);
        System.out.println("Config:"+"\n"+nacosConfig.group+nacosConfig.dataIds+nacosConfig.NUM);
    }

    @Override public void shutdown() {

    }

    @Override public ChaosNode createChaosNode(String node, List<String> nodes) {
        return null;
    }

    @Override public MetaNode createChaosMetaNode(String node, List<String> nodes) {
        return null;
    }

    @Override
    public String getStateName() {
        return null;
    }



    public List<String> getgroup() {
        return nacosConfig.group;
    }
    public List<String> getdataIds(){
        return nacosConfig.dataIds;
    }
    public int getNUM() {return nacosConfig.NUM;}
}


