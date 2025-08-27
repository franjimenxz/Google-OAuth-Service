package com.example.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AirtableConfig {

    @Bean
    WebClient airtableClient(
            WebClient.Builder builder,
            @Value("${airtable.apiBase}") String apiBase,
            @Value("${airtable.token}") String token
    ) {
        return builder
                .baseUrl(apiBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}