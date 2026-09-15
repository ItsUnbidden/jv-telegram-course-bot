package com.unbidden.telegramcoursesbot.repository.impl;

import org.springframework.stereotype.Repository;

import com.unbidden.telegramcoursesbot.config.properties.SchedulingProperties;
import com.unbidden.telegramcoursesbot.service.model.HomeworkFeedbackSession;

@Repository
public class InMemoryHomeworkFeedbackSessionRepository extends InMemoryPagedRequestSessionRepository<HomeworkFeedbackSession> {
    public InMemoryHomeworkFeedbackSessionRepository(SchedulingProperties schedulingProperties) {
        super(schedulingProperties.feedbackSession().expiration());
    }
}
