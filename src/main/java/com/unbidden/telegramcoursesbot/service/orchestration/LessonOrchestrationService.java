package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

import org.springframework.stereotype.Service;

import com.unbidden.telegramcoursesbot.dto.LessonResponseDto;
import com.unbidden.telegramcoursesbot.mapper.LessonMapper;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonOrchestrationService {
    private final LessonService lessonService;

    private final LessonMapper mapper;

    private final EntityUtil entityUtil;

    public LessonResponseDto getById(UserEntity user, Bot bot, Long lessonId) {
        return mapper.toDto(entityUtil.getLessonById(user, bot, lessonId));
    }

    public List<LessonResponseDto> getCourseLessons(Long courseId) {
        return lessonService.getCourseLessons(courseId).stream().map(mapper::toDto).toList();
    }

    public long countByCourse(Long courseId) {
        return lessonService.countByCourse(courseId);
    }
}
