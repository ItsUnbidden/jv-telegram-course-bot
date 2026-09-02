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
public class UpdateCourseRefundStageButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final ContentSessionService sessionService;

    private final CourseOrchestrationService courseService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Course course = entityUtil.getCourseById(botRole, Long.parseLong(params.get(COURSE_ID_PARAM)));

        if (!course.getInvoice().getClass().equals(TelegramInvoice.class)) {
            throw new ForbiddenOperationException("Course " + course.getId() + " uses external payments. Payment type "
                    + "must be changed first before updating its refund stage.", localizationLoader.localize(
                        Localizations.Error.COURSE_REFUND_STAGE_UPDATE_EXTERNAL_INVOICE, botRole));
        }
   
        sessionService.createSession(botRole, p -> {
            courseService.updateRefundStage(p.botRole(), course.getId(), p.messages());
        }, true);

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.NEW_REFUND_STAGE_REQUEST,
                botRole, new Localizations.Service.NewRefundStageRequestParams(contentService.getLocalizedText(botRole, course.getTitle()))));
    }
}
