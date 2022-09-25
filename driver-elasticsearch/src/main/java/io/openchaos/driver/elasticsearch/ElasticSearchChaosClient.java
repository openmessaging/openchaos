package io.openchaos.driver.elasticsearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openchaos.common.InvokeResult;
import io.openchaos.driver.kv.KVClient;
import io.openchaos.driver.elasticsearch.core.Document;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class ElasticSearchChaosClient implements KVClient {
    private static final Logger log = LoggerFactory.getLogger(ElasticSearchChaosClient.class);
    private RestClient esClient;
    private final String endpoint = "openchaos";
    private static final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String KEY = "openchaosTest";

    public ElasticSearchChaosClient(RestClient client) {
        esClient = client;
    }

    @Override
    public void start() {
    }

    @Override
    public void close() {
    }

    @Override
    public InvokeResult put(Optional<String> key, String value) {
        try {
            String id = "/" + KEY + value;
            String method = "POST";
            Request request = new Request(method, "/" + endpoint + "/_create" + id);
            String jsonStr = serialize(key, value);
            NStringEntity entity = new NStringEntity(jsonStr, ContentType.APPLICATION_JSON);
            request.setEntity(entity);
            Response response = esClient.performRequest(request);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
                log.error("Method failed: " + response.getStatusLine());
            } else {
                return InvokeResult.SUCCESS;
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return InvokeResult.FAILURE;
    }

    @Override
    public List<String> getAll(Optional<String> key, int putInvokeCount) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < putInvokeCount; ++i) {
            Document document = fullTextMatch(key, i);
            values.add(document.getValue());
        }
        return values;
    }

    @Override
    public List<String> getAll(Optional<String> key) {
        return null;
    }

    private String serialize(Optional<String> key, String value) throws JsonProcessingException {
        Document document = new Document(String.valueOf(key), value);
        document.setKey(String.valueOf(key));
        document.setValue(value);
        return objectMapper.writeValueAsString(document);
    }

    private List<Document> deserialize(HttpEntity entity) throws IOException {
        String responseBody = EntityUtils.toString(entity);
        JSONObject resultObject = JSON.parseObject(responseBody);
        JSONObject hitsObject = JSON.parseObject(resultObject.get("hits").toString());
        JSONArray jsonArray = JSON.parseArray(hitsObject.get("hits").toString());

        List<Document> documents = new ArrayList<>();
        for (Object object : jsonArray) {
            JSONObject jsonObject1 = JSON.parseObject(object.toString());
            JSONObject jsonObject2 = JSON.parseObject(jsonObject1.get("_source").toString());
            Document document = new Document(String.valueOf(jsonObject2.get("key")), String.valueOf(jsonObject2.get("value")));
            documents.add(document);
        }
        return documents;
    }

    /**
     *    Full text match.
     */
    private Document fullTextMatch(Optional<String> key, int i) {
        Map<String, Object> jsonMap = new HashMap<>();

        Map<String, Object> matchMap = new HashMap<>();
        Map<String, String> fieldsMap = new HashMap<>();
        fieldsMap.put("value", String.valueOf(i));
        matchMap.put("match", fieldsMap);
        jsonMap.put("query", matchMap);

        JSONObject jsonObject = new JSONObject(jsonMap);
        String jsonString = jsonObject.toString();

        Request request = new Request("GET", "/" + endpoint + "/_search");
        request.addParameter("pretty", "true");
        request.setEntity(new NStringEntity(jsonString, ContentType.APPLICATION_JSON));
        try {
            Response response = esClient.performRequest(request);
            return deserialize(response.getEntity()).get(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setEsClient(RestClient esClient) {
        this.esClient = esClient;
    }
}
