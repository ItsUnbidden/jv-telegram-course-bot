package com.unbidden.telegramcoursesbot.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.lang.Nullable;

import com.unbidden.telegramcoursesbot.config.MapperConfig;
import com.unbidden.telegramcoursesbot.dto.HomeworkResponseDto;
import com.unbidden.telegramcoursesbot.exception.MediaTypeParseException;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;

@Mapper(config = MapperConfig.class)
public interface HomeworkMapper {
    static final String NO_MEDIA_TYPES_STR = "NULL";

    @Mapping(target = "mappingId", source = "mapping.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    HomeworkResponseDto toDto(Homework homework);

    static List<MediaType> parseMediaTypes(@Nullable String mediaTypesStr) throws MediaTypeParseException {
        final List<MediaType> mediaTypes = new ArrayList<>();

        if (mediaTypesStr == null || mediaTypesStr.isBlank() || mediaTypesStr.equals(NO_MEDIA_TYPES_STR)) return mediaTypes;

        final String[] mediaTypesStrArray = mediaTypesStr.split(" ");

        for (final String mediaTypeStr : mediaTypesStrArray) {
            try {
                mediaTypes.add(MediaType.valueOf(mediaTypeStr));
            } catch (IllegalArgumentException e) {
                throw new MediaTypeParseException("Unable to parse media type " + mediaTypeStr + ".", e);
            }
        }
        
        return mediaTypes;
    }

    static String parseMediaTypesToString(@Nullable List<MediaType> types) {
        if (types == null || types.isEmpty()) return null;

        return types.stream().map(t -> t.toString()).collect(Collectors.joining(" "));
    }
}
