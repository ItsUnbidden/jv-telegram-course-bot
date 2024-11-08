package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.MoveContentException;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private static final Logger LOGGER = LogManager.getLogger(LessonServiceImpl.class);

    private static final String ERROR_MAPPING_NOT_IN_LESSON = "error_mapping_not_in_lesson";
    private static final String ERROR_LESSON_NOT_FOUND = "error_lesson_not_found";

    private final LessonRepository lessonRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final LocalizationLoader localizationLoader;

    @Override
    @NonNull
    public Lesson save(@NonNull Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public List<Lesson> saveAll(@NonNull List<Lesson> lessons) {
        return lessonRepository.saveAll(lessons);
    }

    @Override
    @NonNull
    public Lesson addContent(@NonNull Long lessonId, @NonNull LocalizedContent content,
            @NonNull UserEntity user) {
        final Lesson lesson = getById(lessonId, user);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(lesson.getStructure().size());
        contentMapping.setContent(List.of(content));
        contentMapping.setTextEnabled(true);
        lesson.getStructure().add(contentMappingRepository.save(contentMapping));
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public Lesson removeContent(@NonNull Long lessonId, @NonNull Long mappingId,
            @NonNull UserEntity user) {
        final Lesson lesson = getById(lessonId, user);

        lesson.getStructure().removeIf(m -> m.getId().equals(mappingId));
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public Lesson getById(@NonNull Long lessonId, @NonNull UserEntity user) {
        return lessonRepository.findById(lessonId).orElseThrow(() -> new EntityNotFoundException(
                "Lesson with id " + lessonId + " does not exist", localizationLoader
                .getLocalizationForUser(ERROR_LESSON_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public Lesson moveContentToIndex(@NonNull Long lessonId, @NonNull Long mappingId, int index,
            @NonNull UserEntity user) throws MoveContentException {
        final Lesson lesson = getById(lessonId, user);
        final List<ContentMapping> potentialMapping = lesson.getStructure().stream()
                .filter(m -> m.getId().equals(mappingId)).toList();
        if (potentialMapping.size() == 0) {
            throw new EntityNotFoundException("Mapping " + mappingId
                    + " does not belong to the lesson " + lessonId, localizationLoader
                    .getLocalizationForUser(ERROR_MAPPING_NOT_IN_LESSON, user));
        }
        final ContentMapping mapping = potentialMapping.get(0);
        if (mapping.getPosition().equals(index)) {
            throw new MoveContentException("Mapping " + mappingId
                    + "'s position is already set to " + index);
        }
        LOGGER.debug("Current mapping order for lesson " + lessonId + ": "
                + lesson.getStructure() + ". Changing mapping " + mappingId
                + "'s position to " + index + "...");

        lesson.getStructure().remove(mapping);
        lesson.getStructure().add(index, mapping);
        for (int i = 0; i < lesson.getStructure().size(); i++) {
            lesson.getStructure().get(i).setPosition(i);
        }
        LOGGER.debug("Updated mapping order for lesson " + lessonId + ": "
                + lesson.getStructure() + ". Persisting...");
        contentMappingRepository.saveAll(lesson.getStructure());
        LOGGER.debug("Mapping positions saved.");
        return lesson;
    }
}
