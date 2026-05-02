package com.jircik.calorietrackerapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient fatSecretApiWebClient(
            @Value("${fatsecret.worker-url}") String workerUrl,
            @Value("${fatsecret.proxy-key}") String proxyKey) {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl(workerUrl)
                .defaultHeader("X-Proxy-Key", proxyKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
