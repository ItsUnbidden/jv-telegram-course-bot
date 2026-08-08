package com.unbidden.telegramcoursesbot.service.timing.executor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BanTriggerExecutor implements TriggerExecutor {
    private static final Logger LOGGER = LogManager.getFormatterLogger(BanTriggerExecutor.class);

    private final TimingService timingService;

    private final UserService userService;

    @Override
    @Transactional
    public void findAndExecute() {
        final List<BanTrigger> triggers = timingService.findAndRemoveExpiredBanTriggers();
        
        for (final BanTrigger trigger : triggers) {
            if (trigger.isGeneral()) {
                userService.liftGeneralBan(null, trigger.getUser());
            } else {
                userService.liftBanInBot(null, trigger.getUser(), trigger.getBot());
            }
        }

        LOGGER.trace(triggers.size() + " expired ban triggers have been executed.");
    }
}
