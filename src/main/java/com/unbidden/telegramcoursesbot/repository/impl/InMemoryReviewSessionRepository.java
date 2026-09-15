package com.unbidden.telegramcoursesbot.repository.impl;

import org.springframework.stereotype.Repository;

import com.unbidden.telegramcoursesbot.config.properties.SchedulingProperties;
import com.unbidden.telegramcoursesbot.service.model.ReviewSession;

@Repository
public class InMemoryReviewSessionRepository extends InMemoryPagedRequestSessionRepository<ReviewSession> {
    public InMemoryReviewSessionRepository(SchedulingProperties schedulingProperties) {
        super(schedulingProperties.reviewSession().expiration());
    }
}
