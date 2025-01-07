package com.unbidden.telegramcoursesbot.service.menu;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
public class MultipageListMeta {
    @NonNull
    private Integer id;

    @NonNull
    private UserEntity user;

    @NonNull
    private Bot bot;

    private int messageId;

    private int amountOfPages;

    private long amountOfElements;

    private int page;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private Function<Map<String, Object>, Localization> localizationFunction;

    @NonNull
    private BiFunction<Integer, Integer, List<String>> dataFunction;

    private boolean isControlMenuUpdateRequired;

    public MultipageListMeta(@NonNull Integer id, @NonNull UserEntity user,
            @NonNull Bot bot, int messageId, int page,
            @NonNull Function<Map<String, Object>, Localization> localizationFunction,
            @NonNull BiFunction<Integer, Integer, List<String>> dataFunction) {
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
