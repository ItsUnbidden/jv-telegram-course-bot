package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoursePriceChangeButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final CourseOrchestrationService courseService;

    private final ContentOrchestrationService contentService;
    
    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));
        final Course course = entityUtil.getCourseById(botRole, courseId);
        
        if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
            throw new ForbiddenOperationException("Course " + courseId + " uses external payments. Payment type "
                    + "must be changed first before updating its price.", localizationLoader.localize(
                        Localizations.Error.COURSE_PRICE_UPDATE_EXTERNAL_INVOICE, botRole));
        }
        final TelegramInvoice invoice = (TelegramInvoice)course.getInvoice();

        sessionService.createSession(botRole, p -> {
            courseService.updateCoursePrice(p.botRole(), courseId, p.messages());
        }, true);

        clientManager.sendMessage(botRole, localizationLoader.localize(
                Localizations.Service.COURSE_PRICE_UPDATE_REQUEST, botRole,
                new Localizations.Service.CoursePriceUpdateRequestParams(contentService
                    .getLocalizedText(botRole, course.getTitle()), invoice.getPrice())));
    }
}
