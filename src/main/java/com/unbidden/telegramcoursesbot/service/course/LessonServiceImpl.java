package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import jakarta.persistence.EntityNotFoundException;
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

    private final LessonRepository lessonRepository;

    private final ContentMappingRepository contentMappingRepository;

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
    public Lesson addContent(@NonNull Long lessonId, @NonNull LocalizedContent content) {
        final Lesson lesson = getById(lessonId);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(lesson.getStructure().size());
        contentMapping.setContent(List.of(content));
        contentMapping.setTextEnabled(true);
        lesson.getStructure().add(contentMappingRepository.save(contentMapping));
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public Lesson removeContent(@NonNull Long lessonId, @NonNull Long mappingId) {
        final Lesson lesson = getById(lessonId);

        lesson.getStructure().removeIf(m -> m.getId().equals(mappingId));
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public Lesson getById(@NonNull Long lessonId) {
        return lessonRepository.findById(lessonId).orElseThrow(() -> new EntityNotFoundException(
                "Lesson with id " + lessonId + " does not exist"));
    }

    @Override
    @NonNull
    public Lesson moveContentToIndex(@NonNull Long lessonId, @NonNull Long mappingId, int index) {
        final Lesson lesson = getById(lessonId);
        final List<ContentMapping> potentialMapping = lesson.getStructure().stream()
                .filter(m -> m.getId().equals(mappingId)).toList();
        if (potentialMapping.size() == 0) {
            throw new EntityNotFoundException("Mapping " + mappingId
                    + " does not belong to the lesson " + lessonId);
        }
        final ContentMapping mapping = potentialMapping.get(0);
        if (mapping.getPosition().equals(index)) {
            throw new InvalidDataSentException("Mapping " + mappingId
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
