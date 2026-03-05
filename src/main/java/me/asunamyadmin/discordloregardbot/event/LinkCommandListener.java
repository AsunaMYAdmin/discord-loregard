package me.asunamyadmin.discordloregardbot.event;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import me.asunamyadmin.discordloregardbot.web.DiscordProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class LinkCommandListener implements EventListener<ChatInputInteractionEvent>{
    private final WebClient webClient;

    @Autowired
    public LinkCommandListener(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Class<ChatInputInteractionEvent> getEventType() {
        return ChatInputInteractionEvent.class;
    }

    @Override
    public Mono<Void> execute(ChatInputInteractionEvent event) {
        if (!event.getCommandName().equalsIgnoreCase("link")) {
            return Mono.empty();
        }

        String token = event.getOption("token")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(null);

        if (token == null) {
            return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .content("Вы должны привязать токен! /link <token>")
                            .ephemeral(true)
                    .build());
        }

        return webClient.post()
                .uri("/api/discord/profile/checkToken")
                .bodyValue(token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return event.reply(
                                InteractionApplicationCommandCallbackSpec.builder()
                                        .content("❌ Токен не найден.")
                                        .ephemeral(true)
                                        .build()
                        );
                    }
                    String discordId = event.getInteraction()
                            .getUser()
                            .getId()
                            .asString();

                    DiscordProfile profile = new DiscordProfile(
                        null,
                        discordId,
                        token,
                        LocalDateTime.now()
                    );

                    return webClient.post()
                            .uri("/api/discord/profile/create")
                            .bodyValue(profile)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .then(
                                    event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                                    .content("✅ Токен верный! Аккаунт привязан.")
                                                    .ephemeral(true)
                                            .build())
                            );

                })
                .onErrorResume(_ ->
                        event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                        .content("⚠ Ошибка соединения с сайтом.")
                                        .ephemeral(true)
                                .build())

                );
    }
}
