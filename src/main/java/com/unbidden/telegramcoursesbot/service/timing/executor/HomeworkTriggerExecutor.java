package com.unbidden.telegramcoursesbot.service.timing.executor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.HomeworkTrigger;
import com.unbidden.telegramcoursesbot.service.orchestration.HomeworkOrchestrationService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor
public class HomeworkTriggerExecutor implements TriggerExecutor {
    private static final Logger LOGGER = LogManager.getFormatterLogger(HomeworkTriggerExecutor.class);

    private final TimingService timingService;

    private final HomeworkOrchestrationService homeworkService;
    
    @Override
    public void findAndExecute() {
        final List<HomeworkTrigger> triggers = timingService.findAndRemoveExpiredHomeworkTriggers();

        triggers.forEach(t -> homeworkService.sendHomework(t.getBotRole(), t.getProgress()));
        LOGGER.trace(triggers.size() + " expired homework triggers have been executed.");
    }
}
