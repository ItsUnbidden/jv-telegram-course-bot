package com.unbidden.telegramcoursesbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("tcp.scheduling")
public record SchedulingProperties(
    Delay autoClearing,
    Delay timedTrigger,
    Expiration session,
    Expiration multipageMeta,
    Expiration reviewSession,
    Expiration feedbackSession
) {
    public record Delay(@NotNull @Min(1000) Long delay) {}
    public record Expiration(@NotNull @Min(10) Integer expiration) {}
}
