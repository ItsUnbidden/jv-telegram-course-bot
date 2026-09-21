package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.dto.internal.HomeworkReceiverWithCountDto;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.exception.MediaTypeParseException;
import com.unbidden.telegramcoursesbot.exception.OnMaintenanceException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.mapper.HomeworkMapper;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress.Status;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final CourseProgressRepository courseProgressRepository;

    private final BotRoleRepository botRoleRepository;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final EntityUtil entityUtil;

    @Transactional(readOnly = true)
    public List<HomeworkProgress> getPendingHomeworksByBotForCurator(Long botId, Long curatorRoleId, Pageable pageable) {
        Assert.notNull(botId, "botId cannot be null");

        final List<HomeworkProgress> progresses = homeworkProgressRepository.findPendingFeedbackByBotId(botId, curatorRoleId, pageable);

        if (!progresses.isEmpty()) contentMappingRepository.findAllById(progresses.stream()
                .map(p -> p.getHomework().getLesson().getCourse().getTitle().getId())
                .toList());

        return progresses;
    }

    @Transactional(readOnly = true)
    public List<HomeworkProgress> getPendingHomeworksByCourseForCurator(Long courseId, Long curatorRoleId, Pageable pageable) {
        Assert.notNull(courseId, "courseId cannot be null");

        final List<HomeworkProgress> progresses = homeworkProgressRepository.findPendingFeedbackByCourseId(courseId, curatorRoleId, pageable);

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

    @Transactional
    public Optional<BotRole> resolveCurator(BotRole botRole, Long courseId) {
        final CourseProgress courseProgress = entityUtil.getCourseProgressForUser(botRole, courseId);

        if (courseProgress.getCurator() != null) return Optional.of(courseProgress.getCurator());

        final Optional<BotRole> potentialCurator = botRoleRepository.findHomeworkReceiverInBot(botRole.getBot().getId());
        
        if (potentialCurator.isPresent()) courseProgress.setCurator(potentialCurator.get());

        return potentialCurator;
    }

    @Transactional
    public Map<BotRole, Integer> reassignCourseProgressesForUser(BotRole current, Long targetBotRole) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetBotRole, "targetBotRole cannot be null");

        final List<CourseProgress> progresses = courseProgressRepository.findByCuratorId(targetBotRole);
        final Map<BotRole, Integer> resultMap = new HashMap<>(); 

        if (progresses.isEmpty()) return resultMap;

        final List<HomeworkReceiverWithCountDto> mentors = botRoleRepository.findHomeworkReceiversWithCountsInBotAndExcludeUser(
                current.getBot().getId(), targetBotRole);  

        LOGGER.info("Reassigning course progresses that were curated by user " + progresses.getFirst().getCurator().getId()
                + " in bot " + current.getBot().getId() + "...");
        if (mentors.isEmpty()) {
            final BotRole creator = entityUtil.getCreator(current.getBot().getId());

            LOGGER.info("There are currently no users in bot " + current.getBot().getId()
                    + " who are receiving homework. All progresses will be assigned to creator " + creator.getId() + ".");

            progresses.forEach(p -> p.setCurator(creator));
            resultMap.put(creator, progresses.size());
        } else {
            mentors.forEach(m -> resultMap.put(m.getCurator(), 0));
            for (final CourseProgress progress : progresses) {  
                final HomeworkReceiverWithCountDto min = mentors.stream().min((o1, o2) -> {
                    if (o1.getNumberOfAssignees() > o2.getNumberOfAssignees()) return 1;
                    if (o1.getNumberOfAssignees() < o2.getNumberOfAssignees()) return -1;
                    return 0;
                }).get();
    
                progress.setCurator(min.getCurator());
                min.setNumberOfAssignees(min.getNumberOfAssignees() + 1);
                resultMap.put(min.getCurator(), resultMap.get(min.getCurator()) + 1);
            }
        }

        return resultMap;
    }

    @Transactional
    public HomeworkProgress transferHomework(BotRole current, Long targetRoleId, Long homeworkProgressId) {
        Assert.notNull(current, "current cannot be null");
        Assert.notNull(targetRoleId, "targetRoleId cannot be null");
        Assert.notNull(homeworkProgressId, "courseId cannot be null");

        final BotRole targetBotRole = entityUtil.getBotRoleById(current, targetRoleId);

        if (!targetBotRole.isReceivingHomework()) {
            throw new ForbiddenOperationException("Unable to transfer assignee to user " + targetBotRole.getUser().getId()
                    + " because they cannot receive homework.", loader.localize(
                        Localizations.Error.HOMEWORK_TRANSFER_USER_DOES_NOT_RECEIVE_HOMEWORK, current));
        }
        final HomeworkProgress homeworkProgress = entityUtil.getHomeworkProgressById(current, homeworkProgressId);
        final UserEntity user = homeworkProgress.getUser();
        final Course course = homeworkProgress.getHomework().getLesson().getCourse();
        final CourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(user.getId(),
                course.getId()).orElseThrow(() -> new EntityNotFoundException("Unable to find a course progress for user "
                + user.getId() + " and course " + course.getId() + ".", loader.localize(Localizations.Error.COURSE_PROGRESS_NOT_FOUND, targetBotRole)));

        LOGGER.info("User " + current.getUser().getId() + " is transfering user " + user.getId()
                + " who is assigned to them in course " + course.getId() + " to user "
                + targetBotRole.getUser().getId() + ".");

        courseProgress.setCurator(targetBotRole);

        return homeworkProgress;
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
