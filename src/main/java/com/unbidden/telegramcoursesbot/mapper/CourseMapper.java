package com.unbidden.telegramcoursesbot.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.unbidden.telegramcoursesbot.config.MapperConfig;
import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;

@Mapper(config = MapperConfig.class)
public interface CourseMapper {
    @Mapping(target = "titleId", source = "course.title.id")
    @Mapping(target = "descriptionId", source = "course.description.id")
    @Mapping(target = "endId", source = "course.endMapping.id")
    @Mapping(target = "botId", source = "course.bot.id")
    @Mapping(target = "lessonIds", source = "course.lessons")
    @Mapping(target = "externalStorePageUrl", source = "course.externalInvoice.externalStorePageUrl")
    @Mapping(target = "externalInvoiceMappingId", source = "course.externalInvoice.mapping.id")
    CourseResponseDto toDto(Course course, String localizedTitle);

    default List<Long> mapLessons(List<Lesson> lessons) {
        return lessons.stream().map(l -> l.getId()).toList();
    }
}
