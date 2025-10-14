package com.unbidden.telegramcoursesbot.bot;

import com.unbidden.telegramcoursesbot.model.Bot;
import org.springframework.lang.NonNull;

public interface ClientManager {
    @NonNull
    CustomTelegramClient getClient(@NonNull Bot bot);

    @NonNull
    BotFatherClient getBotFatherClient();

    @NonNull
    CustomTelegramClient addClient(@NonNull Bot bot);

    @NonNull
    BotFatherClient addBotFatherClient(@NonNull Bot bot);

    void removeClient(@NonNull Bot bot);

    boolean toggleMaintenance();

    boolean isOnMaintenance();

    boolean isRefreshing();

    void setRefreshing(boolean isRefreshing);
}
