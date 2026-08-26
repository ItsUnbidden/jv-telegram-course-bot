package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.content.Content.MediaType;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.CourseOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.ValidatorUtil;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateCourseButtonHandler extends AbstractButtonHandler {
    private static final String PAYMENT_TYPE_PARAM = "terminal";

    private final ContentSessionService sessionService;

    private final CourseOrchestrationService courseService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ValidatorUtil validatorUtil;

    @Override
    @Security(authorities = AuthorityType.COURSE_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final PaymentType paymentType = PaymentType.valueOf(params.get(PAYMENT_TYPE_PARAM));

        sessionService.createSession(user, bot, p -> {
            validatorUtil.checkAtLeastExpectedMessages(p.user(), p.messages(), 1);
            validatorUtil.checkTextLength(p.user(), p.messages().getFirst(), 3, 35);

            final String languageCode;
            if (p.messages().size() > 1 && validatorUtil.checkLanguageCode(user, p.messages().getLast())) {
                languageCode = p.messages().getLast().getText();
                p.messages().removeLast();
            } else {
                languageCode = user.getLanguageCode();
            }

            final Long titleContentId = contentService.parseAndPersistContent(user, bot, p.messages(), List.of(MediaType.TEXT)).getId();
            
            sessionService.createSession(p.user(), p.bot(), p2 -> {
                courseService.createCourse(p2.user(), p2.bot(), titleContentId, languageCode, paymentType, p2.messages());
            });

            clientManager.getClient(p.bot()).sendMessage(p.user(), loader.localize(paymentType == PaymentType.TELEGRAM
                    ? Localizations.Service.NEW_COURSE_TELEGRAM_INVOICE_REQUEST
                    : Localizations.Service.NEW_COURSE_EXTERNAL_INVOICE_REQUEST, p.user()));
        });

        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.NEW_COURSE_TITLE_REQUEST, user));
    }
}
