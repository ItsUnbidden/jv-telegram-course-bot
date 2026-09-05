package com.unbidden.telegramcoursesbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("tcp.webhook")
public record WebhookProperties(
    @NotBlank String secret,
    @NotBlank String url,
    String ip,
    @NotNull @Min(1) Integer maxConnections,
    @NotNull Boolean useCertificate
) {}
