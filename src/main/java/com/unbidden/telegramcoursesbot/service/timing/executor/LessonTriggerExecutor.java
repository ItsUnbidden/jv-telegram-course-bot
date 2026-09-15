package com.unbidden.telegramcoursesbot.service.timing.executor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.unbidden.telegramcoursesbot.model.LessonTrigger;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LessonTriggerExecutor implements TriggerExecutor {
    private static final Logger LOGGER = LogManager.getFormatterLogger(LessonTriggerExecutor.class);

    private final TimingService timingService;

    private final CourseOrchestrationService courseService;

    @Override
    public void findAndExecute() {
        final List<LessonTrigger> triggers = timingService.findAndRemoveExpiredLessonTriggers();

        triggers.forEach(t -> courseService.current(t.getBotRole(), t.getProgress().getCourse().getId()));
        LOGGER.trace(triggers.size() + " expired lesson triggers have been executed.");
    }
}
