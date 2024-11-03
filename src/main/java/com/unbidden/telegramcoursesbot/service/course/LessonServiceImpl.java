package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {
    private final LessonRepository lessonRepository;

    private final ContentMappingRepository contentMappingRepository;

    @Override
    @NonNull
    public Lesson save(@NonNull Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    @Override
    @NonNull
    public Lesson addContent(@NonNull Long lessonId, @NonNull LocalizedContent content) {
        final Lesson lesson = getById(lessonId);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(lesson.getStructure().size());
        contentMapping.setContent(List.of(content));
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'moveContentToIndex'");
    }
}
