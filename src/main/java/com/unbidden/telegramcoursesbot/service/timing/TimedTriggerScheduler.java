package com.unbidden.telegramcoursesbot.service.timing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.service.timing.executor.TriggerExecutor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TimedTriggerScheduler {
    private static final Logger LOGGER = LogManager.getFormatterLogger(TimedTriggerScheduler.class);

    private static final int INITIAL_TIMED_TRIGGER_CHECK_DELAY = 10000;

    private final List<TriggerExecutor> executors;
    
    @Scheduled(initialDelay = INITIAL_TIMED_TRIGGER_CHECK_DELAY,
            fixedRateString = "${telegram.bot.message.course.trigger.schedule.delay}")
    public void checkTriggers() {
        LOGGER.trace("A scheduled check for expired timed triggers is commencing...");
        
        executors.forEach(e -> e.findAndExecute());
    }
}
