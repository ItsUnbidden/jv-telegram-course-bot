package com.unbidden.telegramcoursesbot.config.properties;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Validated
@ConfigurationProperties("tcp.localizations")
public record LocalizationsProperties(
    @NotNull Path path,
    @NotBlank @Pattern(regexp = "^.[a-z]+$") String format,
    @NotBlank @Pattern(regexp = "^[a-z]+(?:,\\s*[a-z]+)*$") String languagePriority
) {}
