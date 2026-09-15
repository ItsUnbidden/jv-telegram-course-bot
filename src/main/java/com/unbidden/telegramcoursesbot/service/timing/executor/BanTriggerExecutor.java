package com.unbidden.telegramcoursesbot.service.timing.executor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.service.orchestration.UserOrchestrationService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BanTriggerExecutor implements TriggerExecutor {
    private static final Logger LOGGER = LogManager.getFormatterLogger(BanTriggerExecutor.class);

    private final TimingService timingService;

    private final UserOrchestrationService userService;

    @Override
    public void findAndExecute() {
        final List<BanTrigger> triggers = timingService.findAndRemoveExpiredBanTriggers();
        
        for (final BanTrigger trigger : triggers) {
            if (trigger.isGeneral()) {
                userService.liftGeneralBan(trigger.getBotRole());
            } else {
                userService.liftBanInBot(trigger.getBotRole());
            }
        }

        LOGGER.trace(triggers.size() + " expired ban triggers have been executed.");
    }
}
