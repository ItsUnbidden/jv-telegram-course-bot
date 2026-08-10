package com.unbidden.telegramcoursesbot.util;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.User;

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
    private static final Long BOT_LORD_ID = 1L;
    private static final Long START_BOT_ID = 2L;

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
    public Course getCourseById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(id, "id cannot be null");
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final Course course = courseRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course " + id + " does not exist",
                localizationLoader.getLocalizationForUser(Error.COURSE_NOT_FOUND, user)));
        
        checkBotVisibility(user, course.getBot(), bot);
        return course;
    }

    @Transactional(readOnly = true)
    public ContentMapping getCourseTitle(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final ContentMapping title = contentMappingRepository.findCourseTitle(courseId).orElseThrow(() ->
                new EntityNotFoundException("Course " + courseId + " does not exist",
                localizationLoader.getLocalizationForUser(Error.COURSE_NOT_FOUND, user)));
        
        if (!title.getContent().isEmpty()) checkBotVisibility(user, title.getContent().getFirst().getBot(), bot);
        return title;
    }

    @Transactional(readOnly = true)
    public Lesson getLessonById(UserEntity user, Bot bot, Long lessonId) {
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() ->
                new EntityNotFoundException("Lesson with id " + lessonId + " does not exist",
                localizationLoader.getLocalizationForUser(Error.LESSON_NOT_FOUND, user)));

        checkBotVisibility(user, lesson.getCourse().getBot(), bot);
        return lesson;
    }

    @Transactional(readOnly = true)
    public Lesson getLessonByPositionAndCourseId(UserEntity user, Bot bot, int position, Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final Lesson lesson = lessonRepository.findByPositionAndCourseId(position, courseId).orElseThrow(() ->
                new EntityNotFoundException("There is no lesson at position " + position
                + " in course " + courseId, localizationLoader.getLocalizationForUser(Error.LESSON_NOT_FOUND, user)));

        checkBotVisibility(user, lesson.getCourse().getBot(), bot);
        return lesson;
    }

    @Transactional(readOnly = true)
    public CourseProgress getCourseProgressForUser(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final CourseProgress progress = courseProgressRepository.findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course progress for user "
                + user.getFullName() + " and course " + courseId + " does not exist.",
                localizationLoader.getLocalizationForUser(Error.COURSE_PROGRESS_NOT_FOUND, user)));
        
        checkBotVisibility(user, progress.getCourse().getBot(), bot);
        return progress;
    }

    @Transactional(readOnly = true)
    public CourseProgress getCourseProgressById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(id, "id cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final CourseProgress progress = courseProgressRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Course progress with id " + id + " does not exist.",
                localizationLoader.getLocalizationForUser(Error.COURSE_PROGRESS_NOT_FOUND, user)));

        checkBotVisibility(user, progress.getCourse().getBot(), bot);
        return progress;
    }

    @Transactional(readOnly = true)
    public Homework getHomeworkById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(id, "id cannot be null");

        final Homework homework = homeworkRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Homework with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(Error.HOMEWORK_NOT_FOUND, user)));

        checkBotVisibility(user, homework.getLesson().getCourse().getBot(), bot);
        return homework;
    }

    @Transactional(readOnly = true)
    public HomeworkProgress getHomeworkProgressById(UserEntity user, Bot bot, Long progressId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(progressId, "progressId cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final HomeworkProgress progress = homeworkProgressRepository.findById(progressId).orElseThrow(() ->
                new EntityNotFoundException("Homework progress with id " + progressId + " does not exist",
                localizationLoader.getLocalizationForUser(Error.HOMEWORK_PROGRESS_NOT_FOUND, user)));

        checkBotVisibility(user, progress.getHomework().getLesson().getCourse().getBot(), bot);
        return progress;
    }

    @Transactional(readOnly = true)
    public HomeworkProgress getHomeworkProgressByHomeworkId(UserEntity user, Bot bot, Long homeworkId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(homeworkId, "homeworkId cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final HomeworkProgress progress = homeworkProgressRepository.findByUserIdAndHomeworkId(
                user.getId(), homeworkId).orElseThrow(() -> new EntityNotFoundException("Homework progress for user "
                + user.getFullName() + " and homework " + homeworkId + " does not exist",
                localizationLoader.getLocalizationForUser(Error.HOMEWORK_PROGRESS_NOT_FOUND, user)));

        checkBotVisibility(user, progress.getHomework().getLesson().getCourse().getBot(), bot);
        return progress;
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
    public UserEntity getUser(User telegramUser) {
        Assert.notNull(telegramUser, "telegramUser cannot be null");

        return userRepository.findById(telegramUser.getId()).orElseThrow(() -> new EntityNotFoundException("User "
                + telegramUser.getId() + " is not registred in the database", localizationLoader
                .loadLocalization(Error.USER_NOT_FOUND, telegramUser.getLanguageCode())));
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(Long userId, String languageCode) {
        Assert.notNull(userId, "userId cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");

        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User "
                + userId + " is not registred in the database", localizationLoader
                .loadLocalization(Error.USER_NOT_FOUND, languageCode)));
    }

    @Transactional(readOnly = true)
    public LocalizedContent getLocalizedContentById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(id, "id cannot be null");
        
        final LocalizedContent content = localizedContentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(Error.CONTENT_NOT_FOUND, user)));

        checkBotVisibility(user, content.getBot(), bot);
        return content;
    }

    @Transactional(readOnly = true)
    public ContentMapping getMappingById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(id, "id cannot be null");

        final ContentMapping mapping = contentMappingRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Content mapping with id " + id
                + " does not exist", localizationLoader.getLocalizationForUser(
                Error.CONTENT_MAPPING_NOT_FOUND, user)));

        if (!mapping.getContent().isEmpty()) checkBotVisibility(user, mapping.getContent().getFirst().getBot(), bot);
        return mapping;
    }

    @Transactional(readOnly = true)
    public Review getReviewByCourseAndUser(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final Review review = reviewRepository.findByCourseIdAndUserId(courseId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User " + user.getId()
                + " has never left a review for course " + courseId, localizationLoader
                .getLocalizationForUser(Error.REVIEW_NOT_FOUND, user)));

        checkBotVisibility(user, review.getCourse().getBot(), bot);
        return review;
    }

    @Transactional(readOnly = true)
    public Review getReviewById(UserEntity user, Bot bot, Long reviewId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(reviewId, "reviewId cannot be null");

        final Review review = reviewRepository.findById(reviewId).orElseThrow(() ->
                new EntityNotFoundException("Review with id " + reviewId + " does not exist.",
                localizationLoader.getLocalizationForUser(Error.REVIEW_NOT_FOUND, user)));

        checkBotVisibility(user, review.getCourse().getBot(), bot);
        return review;
    }

    @Transactional(readOnly = true)
    public BotRole getBotRole(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        return botRoleRepository.findByBotIdAndUserId(bot.getId(), user.getId()).orElseThrow(() ->
                new EntityNotFoundException("Bot role for user " + user.getId()
                + " and bot " + bot.getId() + " does not exist", localizationLoader
                .getLocalizationForUser(Error.BOT_ROLE_NOT_FOUND, user)));
    }

    @Transactional(readOnly = true)
    public SupportRequest getSupportRequestById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(id, "id cannot be null");

        final SupportRequest request = supportRequestRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support request with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(Error.SUPPORT_REQUEST_NOT_FOUND,
                user)));

        checkBotVisibility(user, request.getBot(), bot);
        return request;
    }

    @Transactional(readOnly = true)
    public SupportReply getSupportReplyById(UserEntity user, Bot bot, Long id) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(id, "id cannot be null");

        final SupportReply reply = supportReplyRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Support reply with id " + id + " does not exist",
                localizationLoader.getLocalizationForUser(Error.SUPPORT_REPLY_NOT_FOUND,
                user)));

        checkBotVisibility(user, reply.getBot(), bot);
        return reply;
    }

    @Transactional(readOnly = true)
    public CourseOwnership getCourseOwnership(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = courseOwnershipRepository.findByUserIdAndCourseId(user.getId(), courseId).orElseThrow(() ->
                new EntityNotFoundException("Course ownership for user " + user.getId()
                + " and course " + courseId + " does not exist.", localizationLoader
                .getLocalizationForUser(Error.COURSE_OWNERSHIP_NOT_FOUND, user)));

        checkBotVisibility(user, ownership.getCourse().getBot(), bot);
        return ownership;
    }

    @Transactional(readOnly = true)
    public CourseOwnership getActiveCourseOwnership(UserEntity user, Bot bot, Long courseId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");

        final CourseOwnership ownership = courseOwnershipRepository.findByUserIdAndCourseIdAndStatus(
                user.getId(), courseId, OwnershipStatus.ACTIVE).orElseThrow(() ->
                new EntityNotFoundException("Course ownership for user " + user.getId()
                + " and course " + courseId + " does not exist.", localizationLoader
                .getLocalizationForUser(Error.COURSE_OWNERSHIP_NOT_FOUND, user)));

        checkBotVisibility(user, ownership.getCourse().getBot(), bot);
        return ownership;
    }

    @Transactional(readOnly = true)
    public UserEntity getDiretor() {
        return userRepository.findById(directorId).get();
    }

    @Transactional(readOnly = true)
    public UserEntity getCreator(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        final List<UserEntity> potentialCreator = userRepository
                .findByRoleType(bot.getId(), RoleType.CREATOR, Pageable.unpaged()).toList();

        if (potentialCreator.isEmpty()) {
            return getDiretor();
        }
        return potentialCreator.get(0);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getSupport(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        return userRepository.findByRoleType(bot.getId(), RoleType.SUPPORT, Pageable.unpaged()).toList();
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getMentors(Bot bot) {
        Assert.notNull(bot, "bot cannot be null");

        return userRepository.findByRoleType(bot.getId(), RoleType.MENTOR, Pageable.unpaged()).toList();
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

    @Transactional(readOnly = true)
    public List<Authority> parseAuthorities(List<AuthorityType> types) {
        Assert.notNull(types, "types cannot be null");

        return authorityRepository.findByTypeIn(types);
    }

    public void checkBotLord(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        if (!bot.getId().equals(BOT_LORD_ID)) {
            throw new AccessDeniedException("This action is available only from the "
                    + "bot lord", localizationLoader.getLocalizationForUser(
                    Error.UNAVAILABLE_IN_REGULAR_BOT, user));
        }
    }

    public String getLocalizedTitle(UserEntity localizedFor, Bot bot, UserEntity target) {
        Assert.notNull(localizedFor, "localizedFor cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");

        return localizationLoader.getGenericLocalization(Service.ROLE_TITLE, localizedFor,
                getBotRole(target, bot).getRole().getType().toString().toLowerCase()).getData();
    }

    private void checkBotVisibility(UserEntity user, Bot required, Bot current) {
        if (!required.getId().equals(current.getId()) && !getBotLord().getId().equals(current.getId())) {
            throw new AccessDeniedException("This asset is not available for bot "
                    + current.getId(), localizationLoader.getLocalizationForUser(
                    Error.BOT_VISIBILITY_MISMATCH, user));
        }
    }
}
