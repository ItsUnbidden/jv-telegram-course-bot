package com.unbidden.telegramcoursesbot.mapper;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.lang.Nullable;

import com.unbidden.telegramcoursesbot.config.MapperConfig;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;

@Mapper(config = MapperConfig.class)
public interface HomeworkMapper {
    @Mapping(target = "mappingId", source = "mapping.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    HomeworkResponseDto toDto(Homework homework);

    static List<MediaType> parseMediaTypes(@Nullable String mediaTypesStr) {
        final List<MediaType> mediaTypes = new ArrayList<>();

        if (mediaTypesStr == null || mediaTypesStr.isBlank()) return mediaTypes;

        final String[] mediaTypesStrArray = mediaTypesStr.split(" ");

        for (final String mediaTypeStr : mediaTypesStrArray) {
            mediaTypes.add(MediaType.valueOf(mediaTypeStr));
        }
        
        return mediaTypes;
    }
}
