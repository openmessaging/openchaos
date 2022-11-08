package io.openchaos.driver.elasticsearch.core;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.*;
import java.util.List;

public class ElasticSearchFactory {
    private static volatile RestClient client;
    private static String username;
    private static String password;
    private static List<String> nodes;
    private static boolean isSsl;
    private static int port;
    public static void initial(List<String> nodes, String username, String password, boolean isSsl, int port) {
        ElasticSearchFactory.nodes = nodes;
        ElasticSearchFactory.username = username;
        ElasticSearchFactory.password = password;
        ElasticSearchFactory.isSsl = isSsl;
        ElasticSearchFactory.port = port;
    }

    public static RestClient getClient() {
        if (client == null || !client.isRunning()) {
            synchronized (ElasticSearchFactory.class) {
                if (client == null || !client.isRunning()) {
                    HttpHost[] hosts = getHosts(nodes);
                    RestClientBuilder clientBuilder = RestClient
                            .builder(hosts)
                            .setCompressionEnabled(true);
                    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                    if (!isSsl) {
                        clientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider));
                    } else {
                        try {
                            //todo 无法理解KeyStore是什么东西
                            KeyStore trustStore = KeyStore.getInstance("es");
                            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                                    .loadTrustMaterial(trustStore, null);
                            final SSLContext sslContext = sslContextBuilder.build();
                            clientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                    .setDefaultCredentialsProvider(credentialsProvider)
                                    .setSSLContext(sslContext));
                        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    client = clientBuilder.build();
                }
            }
        }
        return client;
    }

    private static HttpHost[] getHosts(List<String> _nodes) {
        String scheme = isSsl ? "https" : "http";
        HttpHost[] hosts = new HttpHost[_nodes.size()];
        for (int i = 0; i < _nodes.size(); ++i) {
            hosts[i] = new HttpHost(_nodes.get(i), port, scheme);
        }
        return hosts;
    }

    public static void close() {
        if (client != null || !client.isRunning()) {
            try {
                client.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
