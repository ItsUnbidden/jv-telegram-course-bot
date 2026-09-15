package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class LessonService {
    private static final Logger LOGGER = LogManager.getLogger(LessonService.class);

    private final ContentService contentService;

    private final LessonRepository lessonRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final LocalizationLoader localizationLoader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<Lesson> getCourseLessons(Long courseId) {
        return lessonRepository.findByCourseIdOrderByPosition(courseId);
    }

    @Transactional(readOnly = true)
    public long countByCourse(Long courseId) {
        return lessonRepository.countByCourseId(courseId);
    }

    @Transactional
    public Lesson addContent(BotRole botRole, Long lessonId, String languageCode, List<Message> messages) {
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(lesson.getStructure().size());
        contentMapping.setContent(List.of(contentService.parseAndPersistContent(botRole, messages, languageCode)));
        lesson.getStructure().add(contentMappingRepository.save(contentMapping));

        return lesson;
    }

    @Transactional
    public Lesson removeContent(BotRole botRole, Long lessonId, Long mappingId) {
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);

        LOGGER.info("Removing content " + mappingId + " from lesson " + lessonId + "...");

        final Optional<ContentMapping> mappingOpt = lesson.getStructure().stream()
                .filter(m -> m.getId().equals(mappingId)).findAny();

        if (mappingOpt.isEmpty()) {
            throw new EntityNotFoundException("Mapping " + mappingId + " is not present in lesson " + lessonId
                    + ".", localizationLoader.localize(Localizations.Error.MAPPING_NOT_IN_LESSON, botRole));
        }
        lesson.getStructure().removeIf(m -> m.getId().equals(mappingId));
        contentMappingRepository.delete(mappingOpt.get());

        return lesson;
    }

    @Transactional
    public Lesson updateDelay(BotRole botRole, Long lessonId, int newDelay) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");

        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);

        LOGGER.debug("Updating lesson delay... Current delay: " + lesson.getDelay() + ".");
        lesson.setDelay(newDelay);

        return lesson;
    }

    @Transactional
    public Lesson moveContentToIndex(BotRole botRole, Long lessonId, Long mappingId, int index) {
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(mappingId, "mappingId cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);
        final List<ContentMapping> potentialMapping = lesson.getStructure().stream()
                .filter(m -> m.getId().equals(mappingId)).toList();

        if (potentialMapping.size() == 0) {
            throw new EntityNotFoundException("Mapping " + mappingId
                    + " does not belong to lesson " + lessonId, localizationLoader
                    .localize(Error.MAPPING_NOT_IN_LESSON, botRole));
        }
        final ContentMapping mapping = potentialMapping.get(0);

        if (mapping.getPosition().equals(index)) {
            throw new InvalidDataSentException("Mapping " + mappingId + " is already at position "
                    + index + " in lesson " + lessonId + ".", localizationLoader.localize(
                        Localizations.Error.SAME_CONTENT_POSITION, botRole));
        }
        LOGGER.debug("Current mapping order for lesson " + lessonId + ": "
                + lesson.getStructure().stream().map(cm -> cm.getId()).toList()
                + ". Changing mapping " + mappingId + "'s position to " + index + "...");

        lesson.getStructure().remove(mapping);
        lesson.getStructure().add(index, mapping);
        for (int i = 0; i < lesson.getStructure().size(); i++) {
            lesson.getStructure().get(i).setPosition(i);
        }
        LOGGER.debug("Updated mapping order for lesson " + lessonId + ": "
                + lesson.getStructure().stream().map(cm -> cm.getId()).toList());

        return lesson;
    }

    @Transactional
    public Lesson createLesson(BotRole botRole, Long courseId, int position) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Course course = entityUtil.getCourseById(botRole, courseId);

        LOGGER.info("Creating a new lesson for course " + course.getId() + "...");
        final Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setDelay(0);
        lesson.setPosition(position);
        lesson.setStructure(List.of());

        course.getLessons().add(position, lesson);
        for (int i = 0; i < course.getLessons().size(); i++) {
            course.getLessons().get(i).setPosition(i);
        }

        lessonRepository.save(lesson);
        LOGGER.info("New lesson " + lesson.getId() + " has been created for course "
                + course.getId() + " on position " + lesson.getPosition() + ".");
        return lesson;
    }

    @Transactional
    public Lesson deleteLesson(BotRole botRole, Long lessonId) {
        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);

        LOGGER.info("Removing lesson " + lesson.getId() + " from course " + lesson.getCourse().getId() + "...");
        lessonRepository.delete(lesson);
        lessonRepository.flush();

        final List<Lesson> allCourseLessons = lessonRepository
                .findByCourseIdOrderByPosition(lesson.getCourse().getId());

        for (int i = 0; i < allCourseLessons.size(); i++) {
            allCourseLessons.get(i).setPosition(i);
        }

        return lesson;
    }
}
