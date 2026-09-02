package com.unbidden.telegramcoursesbot.service.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class HomeworkFeedbackSession extends PagedRequestSession {
    private final Long courseId;

    private final List<Long> progressIds;

    public HomeworkFeedbackSession(Long id, int counter, Long courseId, List<Long> progressIds) {
        super(id, counter);
        this.courseId = courseId;
        this.progressIds = new ArrayList<>(progressIds);
    }
}
