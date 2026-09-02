package com.unbidden.telegramcoursesbot.service.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewSession extends PagedRequestSession {
    private final Long courseId;

    public ReviewSession(Long id, int counter, Long courseId) {
        super(courseId, counter);
        this.courseId = courseId;
    }
}
