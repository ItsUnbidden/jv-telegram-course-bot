package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;

public interface BotService {
    @NonNull
    Bot createBot(@NonNull UserEntity creator, @NonNull String name, @NonNull String token);

    @NonNull
    Bot createInitialBot(@NonNull UserEntity director);

    @NonNull
    Bot createBotFather(@NonNull UserEntity director);

    @NonNull
    List<Bot> initializeBots();

    @NonNull
    Bot initializeBotFather(@NonNull Bot bot);

    void removeBot(@NonNull Bot bot);

    @NonNull
    Bot getBot(@NonNull String name);

    @NonNull
    Bot getBotFather();

    @NonNull
    Bot getInitialBot();

    void checkBotFather(@NonNull Bot bot, @NonNull UserEntity user);

    @NonNull
    List<Bot> getAllBots();
}
