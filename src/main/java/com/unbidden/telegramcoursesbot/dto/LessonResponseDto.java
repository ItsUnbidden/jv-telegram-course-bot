package com.unbidden.telegramcoursesbot.dto;

import java.util.List;

import lombok.Data;

@Data
public class LessonResponseDto {
    private Long id;

    private Integer position;

    private List<Long> mappingIds;

    private Long courseId;

    private Long homeworkId;

    private Long nextLessonButtonTitleMappingId;

    private Integer delay;

    private Long version;
}
