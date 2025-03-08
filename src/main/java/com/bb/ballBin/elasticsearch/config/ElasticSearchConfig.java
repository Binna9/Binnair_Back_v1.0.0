package com.bb.ballBin.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String serverUrl;

    @Bean
    public RestClient getRestClient() {

        String[] urlParts = serverUrl.replace("http://", "").replace("https://", "").split(":");
        String host = urlParts[0];
        int port = urlParts.length > 1 ? Integer.parseInt(urlParts[1]) : 9200;

        return RestClient.builder(new HttpHost(host, port)).build();
    }

    @Bean
    public ElasticsearchTransport getElasticsearchTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    @Bean
    public ElasticsearchClient getElasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
