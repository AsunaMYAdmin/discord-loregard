package me.asunamyadmin.discordloregardbot.configuration;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import me.asunamyadmin.discordloregardbot.event.EventListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@Configuration
public class BotConfiguration {
    @Value("${token}")
    private String token;

    @Bean
    public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListeners) {
        GatewayDiscordClient client = DiscordClientBuilder.create(token)
                .build()
                .login()
                .block();


        assert client != null;
        long applicationId = Objects.requireNonNull(
                client.getRestClient()
                        .getApplicationId()
                        .block(),
                "Application ID is null"
        );

        Snowflake guildId = Snowflake.of(1478713960231342173L);

        client.getRestClient()
                .getApplicationService()
                .createGuildApplicationCommand(
                        applicationId,
                        guildId.asLong(),
                        ApplicationCommandRequest.builder()
                                .name("link")
                                .description("Привязать аккаунт Loregard. /link token")
                                .addOption(ApplicationCommandOptionData.builder()
                                        .name("token")
                                        .description("Токен с сайта")
                                        .type(3)
                                        .required(true)
                                        .build())
                                .build()
                ).block();

        for (EventListener<T> listener : eventListeners) {
            client.on(listener.getEventType())
                    .flatMap(listener::execute)
                    .onErrorResume(listener::handleError)
                    .subscribe();
        }

        client.onDisconnect().block();

        return client;
    }
}
