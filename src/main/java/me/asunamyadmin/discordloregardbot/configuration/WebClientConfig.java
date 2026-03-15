package me.asunamyadmin.discordloregardbot.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Value("${loregard.bot.token}")
    private String botToken;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl("https://loregard.ru")
                .defaultHeader("Authorization", "Bearer " + botToken)
                .build();
    }
}
