package com.unbidden.telegramcoursesbot.dto;

import java.util.List;

import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;

import lombok.Data;

@Data
public class HomeworkResponseDto {
    private Long id;

    private Long mappingId;

    private List<MediaType> allowedMediaTypes;

    private Long lessonId;

    private Integer delay;

    private boolean isFeedbackRequired;

    private boolean isRepeatedCompletionAvailable;

    private Long version;
}
