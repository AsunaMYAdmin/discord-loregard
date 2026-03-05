package me.asunamyadmin.discordloregardbot.web;

import java.time.LocalDateTime;

public record DiscordProfile(
        Integer profileID,
        String discordID,
        String discordToken,
        LocalDateTime createdAt
) {}
