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
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Lesson;
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

    public LessonResponseDto getById(BotRole botRole, Long lessonId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");

        return mapper.toDto(entityUtil.getLessonById(botRole, lessonId));
    }

    public List<LessonResponseDto> getCourseLessons(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return lessonService.getCourseLessons(courseId).stream().map(mapper::toDto).toList();
    }

    public long countByCourse(Long courseId) {
        Assert.notNull(courseId, "courseId cannot be null");

        return lessonService.countByCourse(courseId);
    }

    public void addContentToLesson(BotRole botRole, Long lessonId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        final String languageCode;
        if (messages.size() > 1 && validatorUtil.checkLanguageCode(botRole, messages.getLast())) {
            languageCode = messages.getLast().getText().trim();
            messages.removeLast();
        } else {
            languageCode = botRole.getUser().getLanguageCode();
        }
        
        LOGGER.debug("Adding content to lesson " + lessonId + "...");
        final Lesson lesson = lessonService.addContent(botRole, lessonId, languageCode, messages);
        
        LOGGER.debug("The new content for code " + languageCode + " has been parsed and a new mapping "
                + lesson.getStructure().getLast().getId() + " has been added to lesson " + lessonId
                + ". Sending confirmation message...");

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LESSON_CONTENT_ADDED, botRole,
                new Localizations.Service.LessonContentAddedParams(lesson.getStructure().getLast().getId(), lessonId)));
        LOGGER.debug("Message sent.");
    }

    public void addLessonToCourse(BotRole botRole, Long courseId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(courseId, "courseId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);

        final long numberOfLessons = countByCourse(courseId);
        final int position = validatorUtil.parseIntInBounds(botRole, messages.get(0), 0, (int)numberOfLessons);

        LOGGER.debug("New position parsed. Adding lesson...");
        lessonService.createLesson(botRole, courseId, position);
        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.NEW_LESSON_CREATED, botRole));
        LOGGER.debug("Message sent.");
    }

    
    public void updateDelay(BotRole botRole, Long lessonId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        LOGGER.info("User " + botRole.getUser().getId() + " is trying to update delay for lesson " + lessonId + "...");
            
        final int newDelay = Math.clamp(validatorUtil.parseIntInBounds(botRole, messages.getFirst(),
                Integer.MIN_VALUE, MAX_LESSON_DELAY), 0, MAX_LESSON_DELAY);
        final Lesson lesson = lessonService.updateDelay(botRole, lessonId, newDelay);

        LOGGER.info("Lesson " + lesson.getId() + " now has a delay of " + lesson.getDelay() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.NEW_DELAY_SET_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
    }

    public void removeMapping(BotRole botRole, Long lessonId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        final Long mappingId = validatorUtil.parseId(botRole, messages.getFirst());

        lessonService.removeContent(botRole, lessonId, mappingId);
        LOGGER.info("Mapping " + mappingId + " has been removed from lesson " + lessonId + ".");

        LOGGER.debug( "Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LESSON_CONTENT_REMOVED, botRole, 
                new Localizations.Service.LessonContentAddedParams(mappingId, lessonId)));
        LOGGER.debug("Message sent.");
    }

    public void deleteLesson(BotRole botRole, Long lessonId, String confirmationPhrase, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 1);
        final String providedStr = validatorUtil.checkText(botRole, messages.getFirst());

        LOGGER.debug("User has provided this string - " + providedStr + ". Checking if this matches the confirmation phrase...");
        if (!confirmationPhrase.equals(providedStr)) {
            throw new InvalidDataSentException("Provided string does not match the confirmation phrase",
                    loader.localize(Localizations.Error.DELETE_LESSON_CONFIRMATION_PHRASE_FAILURE, botRole));
        }
        LOGGER.debug("Confirmation phrase matches. Deleting lesson " + lessonId + "...");

        final Lesson lesson = lessonService.deleteLesson(botRole, lessonId);

        LOGGER.info("Lesson " + lessonId + " has been removed from course " + lesson.getCourse().getId() + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.DELETE_LESSON_SUCCESS, botRole));
        LOGGER.debug("Message sent.");
    }

    public void moveMappingToIndex(BotRole botRole, Long lessonId, List<Message> messages) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(lessonId, "lessonId cannot be null");
        Assert.notEmpty(messages, "messages cannot be null or empty");

        validatorUtil.checkExactExpectedMessages(botRole, messages, 2);
        final long mappingId = validatorUtil.parseId(botRole, messages.getFirst());

        Lesson lesson = entityUtil.getLessonById(botRole, lessonId);
        final int index = validatorUtil.parseIntInBounds(botRole, messages.getLast(),
                0, lesson.getStructure().size() - 1);

        lesson = lessonService.moveContentToIndex(botRole, lessonId, mappingId, index);

        LOGGER.info("Mapping order for lesson " + lesson.getId() + "'s content has been changed.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LESSON_MAPPING_ORDER_CHANGE_SUCCESS, botRole,
                new Localizations.Service.LessonMappingOrderChangeSuccessParams(mappingId, lessonId, index)));
        LOGGER.debug("Message sent.");
    }
}
