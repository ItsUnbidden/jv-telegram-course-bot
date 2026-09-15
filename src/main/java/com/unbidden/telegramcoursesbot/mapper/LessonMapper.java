package com.unbidden.telegramcoursesbot.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.unbidden.telegramcoursesbot.config.MapperConfig;
import com.unbidden.telegramcoursesbot.dto.LessonResponseDto;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

@Mapper(config = MapperConfig.class)
public interface LessonMapper {
    @Mapping(target = "mappingIds", source = "structure")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "homeworkId", source = "homework.id")
    @Mapping(target = "nextLessonButtonTitleMappingId", source = "nextLessonButtonTitle.id")
    LessonResponseDto toDto(Lesson lesson);

    default List<Long> mapMappings(List<ContentMapping> mappings) {
        return mappings.stream().map(m -> m.getId()).toList();
    }
}
