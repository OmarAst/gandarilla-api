package com.riogandarilla.api.configs;

import com.riogandarilla.api.configs.properties.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient whatsappRestClient(AppProperties properties) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(properties.whatsappConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(properties.whatsappReadTimeout());

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
