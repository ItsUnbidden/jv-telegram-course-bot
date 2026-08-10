package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Data;

import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;

@Data
public class MultipageListMeta {
    @NonNull
    private UUID id;

    @NonNull
    private UserEntity user;

    @NonNull
    private Bot bot;

    private int messageId;

    private int numberOfPages;

    private long numberOfElements;

    private int page;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private Function<MultipageListParams, Localization> localizationFunction;

    @NonNull
    private BiFunction<Integer, Integer, Page<String>> dataFunction;

    private boolean isControlMenuUpdateRequired;

    public MultipageListMeta(@NonNull UUID id, @NonNull UserEntity user,
            @NonNull Bot bot, int messageId, int page,
            @NonNull Function<MultipageListParams, Localization> localizationFunction,
            @NonNull BiFunction<Integer, Integer, Page<String>> dataFunction) {
        this.id = id;
        this.user = user;
        this.bot = bot;
        this.messageId = messageId;
        this.page = page;
        this.localizationFunction = localizationFunction;
        this.dataFunction = dataFunction;
        this.createdAt = LocalDateTime.now();
    }
}
