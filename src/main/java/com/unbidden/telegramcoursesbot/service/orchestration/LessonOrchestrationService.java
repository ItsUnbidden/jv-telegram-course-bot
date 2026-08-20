package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.LessonResponseDto;
import com.unbidden.telegramcoursesbot.exception.InvalidDataSentException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.mapper.LessonMapper;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.course.LessonService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(LessonOrchestrationService.class);

    public static final int MAX_LESSON_DELAY = 4320;

    private final LessonService lessonService;

    private final LessonMapper mapper;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    private final ValidatorUtil validatorUtil;

    public LessonResponseDto getById(UserEntity user, Bot bot, Long lessonId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");

        return mapper.toDto(entityUtil.getLessonById(user, bot, lessonId));
    }

    public List<LessonResponseDto> getCourseLessons(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return lessonService.getCourseLessons(courseId).stream().map(mapper::toDto).toList();
    }

    public long countByCourse(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return lessonService.countByCourse(courseId);
    }

    public void addContentToLesson(UserEntity user, Bot bot, Long lessonId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        final String languageCode = validatorUtil.checkLanguageCode(user, messages.getLast()) ? messages.getLast().getText() : user.getLanguageCode();
        
        LOGGER.debug("Adding content to lesson " + lessonId + "...");
        final Lesson lesson = lessonService.addContent(user, bot, lessonId, languageCode, messages);
        
        LOGGER.debug("The new content for code " + languageCode + " has been parsed and a new mapping "
                + lesson.getStructure().getLast().getId() + " has been added to lesson " + lessonId
                + ". Sending confirmation message...");

        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.LESSON_CONTENT_ADDED, user,
                new Localizations.Service.LessonContentAddedParams(lesson.getStructure().getLast().getId(), lessonId)));
        LOGGER.debug("Message sent.");
    }

    public void addLessonToCourse(UserEntity user, Bot bot, Long courseId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);

        final long numberOfLessons = countByCourse(courseId);
        final int position = validatorUtil.parseIntInBounds(user, messages.get(0), 0, (int)numberOfLessons);

        LOGGER.debug("New position parsed. Adding lesson...");
        lessonService.createLesson(user, bot, courseId, position);
        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.NEW_LESSON_CREATED, user));
        LOGGER.debug("Message sent.");
    }

    
    public void updateDelay(UserEntity user, Bot bot, Long lessonId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        LOGGER.info("User " + user.getId() + " is trying to update delay for lesson " + lessonId + "...");
            
        final int newDelay = Math.clamp(validatorUtil.parseIntInBounds(user, messages.getFirst(),
                Integer.MIN_VALUE, MAX_LESSON_DELAY), 0, MAX_LESSON_DELAY);
        final Lesson lesson = lessonService.updateDelay(user, bot, lessonId, newDelay);

        LOGGER.info("Lesson " + lesson.getId() + " now has a delay of " + lesson.getDelay() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.NEW_DELAY_SET_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }

    public void removeMapping(UserEntity user, Bot bot, Long lessonId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        final Long mappingId = validatorUtil.parseId(user, messages.getFirst());

        lessonService.removeContent(user, bot, lessonId, mappingId);
        LOGGER.info("Mapping " + mappingId + " has been removed from lesson " + lessonId + ".");

        LOGGER.debug( "Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(
                Localizations.Service.LESSON_CONTENT_REMOVED, user, 
                new Localizations.Service.LessonContentAddedParams(mappingId, lessonId)));
        LOGGER.debug("Message sent.");
    }

    public void deleteLesson(UserEntity user, Bot bot, Long lessonId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(user, messages, 1);
        final String providedStr = validatorUtil.checkText(user, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    loader.localize(Localizations.Error.DELETE_LESSON_CONFIRMATION_PHRASE_FAILURE, user));
        }
        LOGGER.debug("Confirmation phrase matches. Deleting lesson " + lessonId + "...");

        final Lesson lesson = lessonService.deleteLesson(user, bot, lessonId);

        LOGGER.info("Lesson " + lessonId + " has been removed from course " + lesson.getCourse().getId() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.DELETE_LESSON_SUCCESS, user));
        LOGGER.debug("Message sent.");
    }

    public void moveMappingToIndex(UserEntity user, Bot bot, Long lessonId, List<Message> messages) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(user, messages, 2);
        final long mappingId = validatorUtil.parseId(user, messages.getFirst());

        Lesson lesson = entityUtil.getLessonById(user, bot, lessonId);
        final int index = validatorUtil.parseIntInBounds(user, messages.getLast(),
                0, lesson.getStructure().size() - 1);

        lesson = lessonService.moveContentToIndex(user, bot, lessonId, mappingId, index);

        LOGGER.info("Mapping order for lesson " + lesson.getId() + "'s content has been changed.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.LESSON_MAPPING_ORDER_CHANGE_SUCCESS, user,
                new Localizations.Service.LessonMappingOrderChangeSuccessParams(mappingId, lessonId, index)));
        LOGGER.debug("Message sent.");
    }
}
