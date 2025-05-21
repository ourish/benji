package com.benji.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CoinCapClientConfig {

    @Bean
    public WebClient coinCapClient() {
        return WebClient.builder().build();
    }
}
