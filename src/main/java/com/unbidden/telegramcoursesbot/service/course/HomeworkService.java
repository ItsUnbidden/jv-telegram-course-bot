package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.mapper.HomeworkMapper;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.time.LocalDateTime;
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
public class HomeworkService {
    private static final Logger LOGGER = LogManager.getLogger(HomeworkService.class);
    
    private final HomeworkProgressRepository homeworkProgressRepository;

    private final HomeworkRepository homeworkRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final ContentService contentService;

    private final EntityUtil entityUtil;

    @Transactional
    public HomeworkProgress commit(UserEntity user, Bot bot, Long homeworkId,
            List<Message> messages, Status newStatus) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(messages, "messages cannot be null");
        Assert.notNull(newStatus, "newStatus cannot be null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(user, bot, homeworkId);
        final LocalizedContent content = contentService.parseAndPersistContent(user, bot, messages,
                HomeworkMapper.parseMediaTypes((progress.getHomework().getAllowedMediaTypes())));

        progress.setContent(content);
        updateStatus0(progress, newStatus);

        return progress;
    }

    @Transactional
    public HomeworkProgress approve(UserEntity mentor, Bot bot, UserEntity target,
            Long homeworkId, List<Message> comment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(comment, "comment cannot be null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(target, bot, homeworkId);

        if (!comment.isEmpty()) progress.setLastComment(contentService.parseAndPersistContent(mentor, bot, comment));
        progress.setCurator(mentor);
        updateStatus0(progress, Status.COMPLETED);

        return progress;
    }

    @Transactional
    public HomeworkProgress decline(UserEntity mentor, Bot bot, UserEntity target,
            Long homeworkId, List<Message> comment) {
        Assert.notNull(mentor, "mentor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(comment, "comment cannot be null");

        final HomeworkProgress progress = entityUtil.getHomeworkProgressByHomeworkId(target, bot, homeworkId);

        progress.setLastComment(contentService.parseAndPersistContent(mentor, bot, comment));
        progress.setCurator(mentor);
        updateStatus0(progress, Status.DECLINED);

        return progress;
    }

    @Transactional
    public HomeworkProgress createOrLoadProgress(UserEntity user, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final Optional<HomeworkProgress> potentialProgress = homeworkProgressRepository
                .findByUserIdAndHomeworkIdUnresolved(user.getId(), homeworkId);

        final HomeworkProgress homeworkProgress;
        if (potentialProgress.isEmpty()) {
            LOGGER.debug("User " + user.getId() + " does not have any "
                    + "unresolved homeworks. Creating new homework progress...");
            homeworkProgress = new HomeworkProgress();

            homeworkProgress.setUser(user);
            homeworkProgress.setHomework(homeworkRepository.getReferenceById(homeworkId));
            homeworkProgress.setStatus(Status.INITIALIZED);
            homeworkProgress.setInitializedAt(LocalDateTime.now());
            homeworkProgressRepository.save(homeworkProgress);
        } else {
            LOGGER.debug("User " + user.getId() + " already has an unresolved homework progress.");
            homeworkProgress = potentialProgress.get();
        }
        return homeworkProgress;
    }

    @Transactional
    public Homework updateContent(UserEntity user, Bot bot, Long homeworkId, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        final Homework homework = entityUtil.getHomeworkById(user, bot, homeworkId);
        final ContentMapping contentMapping = new ContentMapping();

        contentMapping.setPosition(0);
        contentMapping.setContent(List.of(entityUtil.getLocalizedContentReference(contentId)));
        homework.setMapping(contentMappingRepository.save(contentMapping));

        return homework;
    }

    @Transactional
    public Homework createDefault(UserEntity user, Bot bot, Long lessonId, Long contentId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(contentId, "contentId cannot be null");

        LOGGER.debug("Creating a new default homework for lesson " + lessonId + "...");

        final Homework homework = new Homework();
        final ContentMapping mapping = new ContentMapping();
        final Lesson lesson = entityUtil.getLessonById(user, bot, lessonId);

        mapping.setContent(List.of(entityUtil.getLocalizedContentReference(contentId)));
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

    private HomeworkProgress updateStatus0(HomeworkProgress progress, Status newStatus) {
        progress.setStatus(newStatus);
        if (newStatus == Status.AWAITS_APPROVAL) progress.setApproveRequestedAt(LocalDateTime.now());
        if (newStatus == Status.COMPLETED) progress.setFinishedAt(LocalDateTime.now());

        return progress;
    }
}
