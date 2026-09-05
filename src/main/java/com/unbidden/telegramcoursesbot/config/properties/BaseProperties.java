package com.unbidden.telegramcoursesbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("tcp.base")
public record BaseProperties(
    @NotBlank String startBotToken,
    @NotBlank String botLordToken,
    @NotNull Long directorId
) {}
