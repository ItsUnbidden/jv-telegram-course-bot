package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.exception.MediaTypeParseException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.mapper.HomeworkMapper;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Service
@RequiredArgsConstructor
public class HomeworkService {
    private static final Logger LOGGER = LogManager.getLogger(HomeworkService.class);
    
    private final HomeworkProgressRepository homeworkProgressRepository;

    private final HomeworkRepository homeworkRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<HomeworkProgress> getPendingHomeworksByBot(Long botId, Pageable pageable) {
        Assert.notNull(botId, "botId cannot be null");

        final List<HomeworkProgress> progresses = homeworkProgressRepository.findPendingFeedbackByBotId(botId, pageable);

        if (!progresses.isEmpty()) contentMappingRepository.findAllById(progresses.stream()
                .map(p -> p.getHomework().getLesson().getCourse().getTitle().getId())
                .toList());

        return progresses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkProgress> getPendingHomeworksByCourse(Long courseId, Pageable pageable) {
        Assert.notNull(courseId, "courseId cannot be null");

        final List<HomeworkProgress> progresses = homeworkProgressRepository.findPendingFeedbackByCourseId(courseId, pageable);

        if (!progresses.isEmpty()) contentMappingRepository.findAllById(progresses.stream()
                .map(p -> p.getHomework().getLesson().getCourse().getTitle().getId())
                .toList());

        return progresses;
    }

    @Transactional
    public HomeworkProgress commit(BotRole botRole, Long homeworkId, List<Message> messages, Status newStatus) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(newStatus, "newStatus cannot be null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(botRole, homeworkId);

        if (progress.getHomework().getLesson().getCourse().isUnderMaintenance()) {
            throw new OnMaintenanceException("Course " + progress.getHomework().getLesson().getCourse().getId() + " is currently "
                    + "marked as under maintenance", loader.localize(
                    Localizations.Error.COURSE_UNDER_MAINTENANCE, botRole));
        }
        try {
            final LocalizedContent content = contentService.parseAndPersistContent(botRole, messages,
                    HomeworkMapper.parseMediaTypes((progress.getHomework().getAllowedMediaTypes())));

            progress.setContent(content);
        } catch (MediaTypeParseException e) {
            throw new RuntimeException("Unable to parse allowed homework media types. This is a bug.");
        }

        updateStatus0(progress, newStatus);

        return progress;
    }

    @Transactional
    public HomeworkProgress approve(BotRole botRole, Long progressId, List<Message> comment) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notNull(comment, "comment cannot be null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressById(botRole, progressId);

        if (!comment.isEmpty()) progress.setLastComment(contentService.parseAndPersistContent(botRole, comment));
        progress.setCurator(botRole.getUser());
        updateStatus0(progress, Status.COMPLETED);

        return progress;
    }

    @Transactional
    public HomeworkProgress decline(BotRole botRole, Long progressId, List<Message> comment) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notEmpty(comment, "comment cannot be empty or null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressById(botRole, progressId);

        progress.setLastComment(contentService.parseAndPersistContent(botRole, comment));
        progress.setCurator(botRole.getUser());
        updateStatus0(progress, Status.DECLINED);

        return progress;
    }

    @Transactional
    public HomeworkProgress createOrLoadProgress(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Optional<HomeworkProgress> potentialProgress = homeworkProgressRepository
                .findByUserIdAndHomeworkIdUnresolved(botRole.getUser().getId(), homeworkId);

        final HomeworkProgress homeworkProgress;
        if (potentialProgress.isEmpty()) {
            LOGGER.debug("User " + botRole.getUser().getId() + " does not have any "
                    + "unresolved homeworks. Creating new homework progress...");
            homeworkProgress = new HomeworkProgress();

            homeworkProgress.setUser(botRole.getUser());
            homeworkProgress.setHomework(entityUtil.getHomeworkById(botRole, homeworkId));
            homeworkProgress.setStatus(Status.INITIALIZED);
            homeworkProgress.setInitializedAt(LocalDateTime.now());
            homeworkProgressRepository.save(homeworkProgress);
        } else {
            LOGGER.debug("User " + botRole.getUser().getId() + " already has an unresolved homework progress.");
            homeworkProgress = potentialProgress.get();
        }
        return homeworkProgress;
    }

    @Transactional
    public Homework updateContent(BotRole botRole, Long homeworkId, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(0);
        contentMapping.setContent(List.of(contentService.parseAndPersistContent(botRole, messages, languageCode)));

        contentMappingRepository.delete(homework.getMapping());
        homework.setMapping(contentMappingRepository.save(contentMapping));

        return homework;
    }

    @Transactional
    public Homework updateDelay(BotRole botRole, Long homeworkId, int newDelay) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);

        LOGGER.debug("Updating homework delay... Current delay: " + homework.getDelay() + ".");
        homework.setDelay(newDelay);

        return homework;
    }

    @Transactional
    public Homework toggleFeedbackInclusion(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);
        
        homework.setFeedbackRequired(!homework.isFeedbackRequired());
        
        LOGGER.info("Feedback inclusion for homework " + homeworkId + " is now " + getStatus(homework.isFeedbackRequired()) + ".");
        
        return homework;
    }

    @Transactional
    public Homework toggleRepeatedCompletion(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);
        
        homework.setRepeatedCompletionAvailable(!homework.isRepeatedCompletionAvailable());
        
        LOGGER.info("Repeated completion for homework " + homeworkId + " is now " + getStatus(homework.isRepeatedCompletionAvailable()) + ".");
        
        return homework;
    }

    @Transactional
    public Homework updateAllowedMediaTypes(BotRole botRole, Long homeworkId, List<MediaType> mediaTypes) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(mediaTypes, "mediaTypes cannot be null");

        final Homework homework = entityUtil.getHomeworkById(botRole, homeworkId);

        homework.setAllowedMediaTypes(HomeworkMapper.parseMediaTypesToString(mediaTypes));

        return homework;
    }

    @Transactional
    public Homework createHomework(BotRole botRole, Long lessonId, String languageCode, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notEmpty(messages, "messages cannot be empty or null");

        final Homework homework = new Homework();
        final ContentMapping mapping = new ContentMapping();
        final Lesson lesson = entityUtil.getLessonById(botRole, lessonId);

        mapping.setPosition(0);

        homework.setAllowedMediaTypes("");
        homework.setFeedbackRequired(true);
        homework.setLesson(lesson);
        homework.setDelay(0);
        homework.setMapping(contentMappingRepository.save(mapping));
        homework.setRepeatedCompletionAvailable(false);

        lesson.setHomework(homework);

        return homeworkRepository.save(homework);
    }

    private String getStatus(boolean status) {
        return status ? "ENABLED" : "DISABLED";
    }

    private HomeworkProgress updateStatus0(HomeworkProgress progress, Status newStatus) {
        progress.setStatus(newStatus);
        if (newStatus == Status.AWAITS_APPROVAL) progress.setApproveRequestedAt(LocalDateTime.now());
        if (newStatus == Status.COMPLETED) progress.setFinishedAt(LocalDateTime.now());

        return progress;
    }
}
