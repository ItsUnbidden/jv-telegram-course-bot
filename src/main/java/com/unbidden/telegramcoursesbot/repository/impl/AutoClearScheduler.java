package com.unbidden.telegramcoursesbot.repository.impl;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.repository.AutoClearable;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AutoClearScheduler {
    private static final Logger LOGGER = LogManager.getFormatterLogger(AutoClearScheduler.class);

    private static final int INITIAL_EXPIRY_CHECK_DELAY = 10000;

    private final List<AutoClearable> autoClearables;

    @Scheduled(initialDelay = INITIAL_EXPIRY_CHECK_DELAY, fixedDelayString = "${telegram.bot.auto-clearing.schedule.delay}")
    public void clear() {
        LOGGER.trace("Starting scheduled auto clearing...");
        autoClearables.forEach(ac -> ac.removeExpired());
        LOGGER.trace("Auto clearing completed.");
    }
}
