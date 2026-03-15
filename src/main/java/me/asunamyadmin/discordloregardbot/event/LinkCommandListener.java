package me.asunamyadmin.discordloregardbot.event;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import lombok.RequiredArgsConstructor;
import me.asunamyadmin.discordloregardbot.web.DiscordProfile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class LinkCommandListener implements EventListener<ChatInputInteractionEvent>{
    private final WebClient webClient;

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
        log.info("Начинаю запрос к сайту");
        return webClient.post()
                .uri("/api/discord/profile/checkToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(token)
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnSuccess(r -> log.info("checkToken вернул: {}", r))
                .doOnError(e -> log.error("checkToken ошибка: {}", e.getMessage()))
                .timeout(Duration.ofSeconds(5))
                .flatMap(isValid -> {
                    log.info("isValid: {}", isValid);
                    if (!isValid) {
                        return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                .content("❌ Токен не найден.")
                                .ephemeral(true)
                                .build());
                    }
                    String discordId = event.getInteraction().getUser().getId().asString();
                    DiscordProfile discordProfile = new DiscordProfile(discordId, token);
                    return webClient.post()
                            .uri("/api/discord/profile/create")
                            .bodyValue(discordProfile)
                            .retrieve()
                            .bodyToMono(Void.class)
                            .then(event.reply(InteractionApplicationCommandCallbackSpec.builder()
                                    .content("✅ Аккаунт привязан!")
                                    .ephemeral(true)
                                    .build()));
                })
                .onErrorResume(e -> {
                    log.error("Ошибка: {}", e.getMessage());
                    return event.reply(InteractionApplicationCommandCallbackSpec.builder()
                            .content("⚠ Ошибка соединения с сайтом.")
                            .ephemeral(true)
                            .build());
                });
    }
}
