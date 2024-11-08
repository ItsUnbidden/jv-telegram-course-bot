package com.unbidden.telegramcoursesbot.util;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class BlockingAspect {
    private final CustomTelegramClient client;

    @Before("@annotation(Blockable)")
    public void blockingAdvice() throws OnMaintenanceException {
        if (client.isOnMaintenance()) {
            throw new OnMaintenanceException("Bot is currently severing all "
                    + "connections and going on maintenance", null);
        }
    }
}
