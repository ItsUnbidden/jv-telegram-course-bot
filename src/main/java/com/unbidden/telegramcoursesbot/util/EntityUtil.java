package com.unbidden.telegramcoursesbot.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.unbidden.telegramcoursesbot.exception.AccessDeniedException;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.localization.Localizations.Service;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Homework;
import com.unbidden.telegramcoursesbot.model.HomeworkProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.Review;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.SupportReply;
import com.unbidden.telegramcoursesbot.model.SupportRequest;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.model.content.ContentMapping;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.repository.AuthorityRepository;
import com.unbidden.telegramcoursesbot.repository.BotRepository;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.ContentMappingRepository;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkProgressRepository;
import com.unbidden.telegramcoursesbot.repository.HomeworkRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.repository.LocalizedContentRepository;
import com.unbidden.telegramcoursesbot.repository.ReviewRepository;
import com.unbidden.telegramcoursesbot.repository.RoleRepository;
import com.unbidden.telegramcoursesbot.repository.SupportReplyRepository;
import com.unbidden.telegramcoursesbot.repository.SupportRequestRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EntityUtil {
    public static final Long BOT_LORD_ID = 1L;
    public static final Long START_BOT_ID = 2L;

    private final CourseRepository courseRepository;

    private final LessonRepository lessonRepository;

    private final BotRepository botRepository;

    private final HomeworkRepository homeworkRepository;

    private final HomeworkProgressRepository homeworkProgressRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final LocalizedContentRepository localizedContentRepository;

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final ContentMappingRepository contentMappingRepository;

    private final AuthorityRepository authorityRepository;

    private final ReviewRepository reviewRepository;

    private final BotRoleRepository botRoleRepository;

    private final SupportRequestRepository supportRequestRepository;

    private final SupportReplyRepository supportReplyRepository;

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final LocalizationLoader localizationLoader;

    @Value("${telegram.bot.authorization.director.id}")
    private Long directorId;

    @Transactional(readOnly = true)
    public Course getCourseById(BotRole botRole, Long id) {
        Assert.notNull(id, "id cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Course course = courseRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course " + id + " does not exist",
                localizationLoader.localize(Error.COURSE_NOT_FOUND, botRole)));
        
        checkBotVisibility(botRole, course.getBot());
        return course;
    }

    @Transactional(readOnly = true)
    public ContentMapping getCourseTitle(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final ContentMapping title = contentMappingRepository.findCourseTitle(courseId).orElseThrow(() ->
                new EntityNotFoundException("Course " + courseId + " does not exist",
                localizationLoader.localize(Error.COURSE_NOT_FOUND, botRole)));
        
        if (!title.getContent().isEmpty()) checkBotVisibility(botRole, title.getContent().getFirst().getBot());
        return title;
    }

    @Transactional(readOnly = true)
    public Lesson getLessonById(BotRole botRole, Long lessonId) {
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() ->
                new EntityNotFoundException("Lesson with id " + lessonId + " does not exist",
                localizationLoader.localize(Error.LESSON_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, lesson.getCourse().getBot());
        return lesson;
    }

    @Transactional(readOnly = true)
    public Lesson getLessonByPositionAndCourseId(BotRole botRole, int position, Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");

        final Lesson lesson = lessonRepository.findByPositionAndCourseId(position, courseId).orElseThrow(() ->
                new EntityNotFoundException("There is no lesson at position " + position
                + " in course " + courseId, localizationLoader.localize(Error.LESSON_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, lesson.getCourse().getBot());
        return lesson;
    }

    @Transactional(readOnly = true)
    public CourseProgress getCourseProgressForUser(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseProgress progress = courseProgressRepository.findByUserIdAndCourseId(botRole.getUser().getId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course progress for user "
                + botRole.getUser().getFullName() + " and course " + courseId + " does not exist.",
                localizationLoader.localize(Error.COURSE_PROGRESS_NOT_FOUND, botRole)));
        
        checkBotVisibility(botRole, progress.getCourse().getBot());
        return progress;
    }

    @Transactional(readOnly = true)
    public CourseProgress getCourseProgressById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");

        final CourseProgress progress = courseProgressRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course progress with id " + id + " does not exist.",
                localizationLoader.localize(Error.COURSE_PROGRESS_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, progress.getCourse().getBot());
        return progress;
    }

    @Transactional(readOnly = true)
    public Homework getHomeworkById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");

        final Homework homework = homeworkRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Homework with id " + id + " does not exist",
                localizationLoader.localize(Error.HOMEWORK_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, homework.getLesson().getCourse().getBot());
        return homework;
    }

    @Transactional(readOnly = true)
    public HomeworkProgress getHomeworkProgressById(BotRole botRole, Long progressId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");

        final HomeworkProgress progress = homeworkProgressRepository.findById(progressId).orElseThrow(() ->
                new EntityNotFoundException("Homework progress with id " + progressId + " does not exist",
                localizationLoader.localize(Error.HOMEWORK_PROGRESS_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, progress.getHomework().getLesson().getCourse().getBot());
        return progress;
    }

    @Transactional(readOnly = true)
    public HomeworkProgress getHomeworkProgressByHomeworkId(BotRole botRole, Long homeworkId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");

        final HomeworkProgress progress = homeworkProgressRepository.findByUserIdAndHomeworkIdUnresolved(
                botRole.getUser().getId(), homeworkId).orElseThrow(() -> new EntityNotFoundException("Homework progress for user "
                + botRole.getUser().getFullName() + " and homework " + homeworkId + " does not exist",
                localizationLoader.localize(Error.HOMEWORK_PROGRESS_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, progress.getHomework().getLesson().getCourse().getBot());
        return progress;
    }

    @Transactional(readOnly = true)
    public BotRole getActiveBotRole(BotRole botRole, Long targetId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return botRoleRepository.findByBotIdAndUserIdAndIsDisabledFalse(botRole.getBot().getId(), targetId).orElseThrow(() ->
                new EntityNotFoundException("An active bot role for user " + targetId
                + " and bot " + botRole.getBot().getId() + " does not exist", localizationLoader
                .localize(Error.BOT_ROLE_NOT_FOUND, botRole)));
    }

    @Transactional(readOnly = true)
    public BotRole getBotRole(BotRole botRole, Long targetId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return botRoleRepository.findByBotIdAndUserId(botRole.getBot().getId(), targetId).orElseThrow(() ->
                new EntityNotFoundException("A bot role for user " + targetId
                + " and bot " + botRole.getBot().getId() + " does not exist", localizationLoader
                .localize(Error.BOT_ROLE_NOT_FOUND, botRole)));
    }

    @Transactional(readOnly = true)
    public BotRole getBotRoleById(BotRole botRole, Long botRoleId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(botRoleId, "botRoleId cannot be null");

        final BotRole targetBotRole = botRoleRepository.findById(botRoleId).orElseThrow(() ->
                new EntityNotFoundException("Bot role " + botRoleId + " does not exist",
                localizationLoader.localize(Error.BOT_ROLE_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, targetBotRole.getBot());

        return targetBotRole;
    }

    @Transactional(readOnly = true)
    public Bot getBot(Long id) {
        Assert.notNull(id, "id cannot be null");
        Assert.state(!id.equals(BOT_LORD_ID), "bot lord cannot be fetched here");

        return botRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("There is no bot with ID " + id, null));
    }

    @Transactional(readOnly = true)
    public Bot getBotLord() {
        return botRepository.findById(BOT_LORD_ID).orElseThrow(() ->
                new EntityNotFoundException("Bot lord has not been created yet", null));
    }

    @Transactional(readOnly = true)
    public Bot getStartBot() {
        return getBot(START_BOT_ID);
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(BotRole botRole, Long userId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(userId, "userId cannot be null");

        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User "
                + userId + " is not registred in the database", localizationLoader
                .localize(Error.USER_NOT_FOUND, botRole)));
    }

    @Transactional(readOnly = true)
    public LocalizedContent getLocalizedContentById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");
        
        final LocalizedContent content = localizedContentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content with id " + id + " does not exist",
                localizationLoader.localize(Error.CONTENT_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, content.getBot());
        return content;
    }

    @Transactional(readOnly = true)
    public ContentMapping getMappingById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");

        final ContentMapping mapping = contentMappingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Content mapping with id " + id
                + " does not exist", localizationLoader.localize(
                Error.CONTENT_MAPPING_NOT_FOUND, botRole)));

        if (!mapping.getContent().isEmpty()) checkBotVisibility(botRole, mapping.getContent().getFirst().getBot());
        return mapping;
    }

    @Transactional(readOnly = true)
    public Review getReviewByCourseAndUser(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Review review = reviewRepository.findByUserIdAndCourseId(botRole.getUser().getId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("User " + botRole.getUser().getId()
                + " has never left a review for course " + courseId, localizationLoader
                .localize(Error.REVIEW_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, review.getCourse().getBot());
        return review;
    }

    @Transactional(readOnly = true)
    public Review getReviewById(BotRole botRole, Long reviewId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");

        final Review review = reviewRepository.findById(reviewId).orElseThrow(() ->
                new EntityNotFoundException("Review with id " + reviewId + " does not exist.",
                localizationLoader.localize(Error.REVIEW_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, review.getCourse().getBot());
        return review;
    }

    @Transactional(readOnly = true)
    public SupportRequest getSupportRequestById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");

        final SupportRequest request = supportRequestRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support request with id " + id + " does not exist",
                localizationLoader.localize(Error.SUPPORT_REQUEST_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, request.getBot());
        return request;
    }

    @Transactional(readOnly = true)
    public SupportReply getSupportReplyById(BotRole botRole, Long id) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(id, "id cannot be null");

        final SupportReply reply = supportReplyRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support reply with id " + id + " does not exist",
                localizationLoader.localize(Error.SUPPORT_REPLY_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, reply.getBot());
        return reply;
    }

    @Transactional(readOnly = true)
    public CourseOwnership getCourseOwnership(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = courseOwnershipRepository.findByUserIdAndCourseId(botRole.getUser().getId(), courseId).orElseThrow(() ->
                new EntityNotFoundException("Course ownership for user " + botRole.getUser().getId()
                + " and course " + courseId + " does not exist.", localizationLoader
                .localize(Error.COURSE_OWNERSHIP_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, ownership.getCourse().getBot());
        return ownership;
    }

    @Transactional(readOnly = true)
    public CourseOwnership getActiveCourseOwnership(BotRole botRole, Long courseId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = courseOwnershipRepository.findByUserIdAndCourseIdAndStatus(
                botRole.getUser().getId(), courseId, OwnershipStatus.ACTIVE).orElseThrow(() ->
                new EntityNotFoundException("Course ownership for user " + botRole.getUser().getId()
                + " and course " + courseId + " does not exist.", localizationLoader
                .localize(Error.COURSE_OWNERSHIP_NOT_FOUND, botRole)));

        checkBotVisibility(botRole, ownership.getCourse().getBot());
        return ownership;
    }

    @Transactional(readOnly = true)
    public UserEntity getDirector() {
        return userRepository.findById(directorId).get();
    }

    @Transactional(readOnly = true)
    public BotRole getDirectorBotRole(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return botRoleRepository.findByBotIdAndRoleTypeAndIsDisabledFalse(botId, RoleType.DIRECTOR).getFirst();
    }

    @Transactional(readOnly = true)
    public BotRole getCreator(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        final List<BotRole> potentialCreator = botRoleRepository.findByBotIdAndRoleTypeAndIsDisabledFalse(botId, RoleType.CREATOR);

        if (potentialCreator.isEmpty()) {
            return botRoleRepository.findByBotIdAndRoleTypeAndIsDisabledFalse(botId, RoleType.DIRECTOR).getFirst();
        }
        return potentialCreator.getFirst();
    }

    @Transactional(readOnly = true)
    public List<BotRole> getSupport(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return botRoleRepository.findByBotIdAndRoleTypeAndIsDisabledFalse(botId, RoleType.SUPPORT);
    }

    @Transactional(readOnly = true)
    public List<BotRole> getMentors(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return botRoleRepository.findByBotIdAndRoleTypeAndIsDisabledFalse(botId, RoleType.MENTOR);
    }

    @Transactional(readOnly = true)
    public Role getRole(RoleType roleType) {
        Assert.notNull(roleType, "roleType cannot be null");
        
        return roleRepository.findByType(roleType).get();
    }

    public LocalizedContent getLocalizedContentReference(Long id) {
        Assert.notNull(id, "id cannot be null");

        return localizedContentRepository.getReferenceById(id);
    }

    public Course getCourseReference(Long id) {
        Assert.notNull(id, "id cannot be null");

        return courseRepository.getReferenceById(id);
    }

    public UserEntity getUserReference(Long id) {
        Assert.notNull(id, "id cannot be null");

        return userRepository.getReferenceById(id);
    }

    public Bot getBotReference(Long id) {
        Assert.notNull(id, "id cannot be null");

        return botRepository.getReferenceById(id);
    }

    public BotRole getBotRoleReference(Long id) {
        Assert.notNull(id, "id cannot be null");

        return botRoleRepository.getReferenceById(id);
    }

    @Transactional(readOnly = true)
    public List<Authority> parseAuthorities(List<AuthorityType> types) {
        Assert.notNull(types, "types cannot be null");

        return authorityRepository.findByTypeIn(types);
    }

    public void checkBotLord(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        if (!isBotLord(botRole.getBot().getId())) {
            throw new AccessDeniedException("This action is available only from the "
                    + "bot lord", localizationLoader.localize(
                    Error.UNAVAILABLE_IN_REGULAR_BOT, botRole));
        }
    }

    public boolean isBotLord(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return botId.equals(BOT_LORD_ID);
    }

    public String getLocalizedTitle(BotRole localizedFor, BotRole target) {
        Assert.notNull(localizedFor, "localizedFor cannot be null");
        Assert.notNull(target, "target cannot be null");

        return localizationLoader.localizeGeneric(Service.ROLE_TITLE, localizedFor,
                target.getRole().getType().toString().toLowerCase()).getData();
    }

    private void checkBotVisibility(BotRole botRole, Bot required) {
        if (!required.getId().equals(botRole.getBot().getId()) && !getBotLord().getId().equals(botRole.getBot().getId())) {
            throw new AccessDeniedException("This asset is not available for bot "
                    + botRole.getBot().getId(), localizationLoader.localize(
                    Error.BOT_VISIBILITY_MISMATCH, botRole));
        }
    }
}
