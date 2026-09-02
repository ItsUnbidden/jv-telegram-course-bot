package com.unbidden.telegramcoursesbot.repository.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.unbidden.telegramcoursesbot.service.model.HomeworkFeedbackSession;

@Repository
public class InMemoryHomeworkFeedbackSessionRepository extends InMemoryPagedRequestSessionRepository<HomeworkFeedbackSession> {
    public InMemoryHomeworkFeedbackSessionRepository(@Value("${telegram.bot.message.feedback-session.expiration}") Integer expiration) {
        super(expiration);
    }
}
