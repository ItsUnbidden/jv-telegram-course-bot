package com.unbidden.telegramcoursesbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("tcp.paged-request")
public record PagedRequestProperties(
    PageSize reviews,
    PageSize homework
) {
    public record PageSize(@NotNull @Min(1) Integer pageSize) {}
}
