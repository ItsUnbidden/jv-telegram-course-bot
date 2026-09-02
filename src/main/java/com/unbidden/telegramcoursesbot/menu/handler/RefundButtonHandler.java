package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.CourseOwnership;
import com.unbidden.telegramcoursesbot.model.TelegramPaymentDetails;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";

    private final PaymentOrchestrationService paymentService;

    private final ContentSessionService sessionService;

    private final ContentOrchestrationService contentService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.REFUND)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));
        final CourseOwnership ownership = paymentService.checkRefundPossible(botRole, courseId);
        final TelegramPaymentDetails paymentDetails = (TelegramPaymentDetails)ownership.getLastPaymentDetails();
        final String courseName = contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId));
        final String confirmationPhrase = localizationLoader.localize(Localizations.Service.REFUND_CONFIRMATION_PHRASE, botRole,
                new Localizations.Service.RefundConfirmationPhraseParams(courseName, paymentDetails.getTotalAmount())).getData();

        sessionService.createSession(botRole, p -> {
            paymentService.refund(p.botRole(), courseId, confirmationPhrase, p.messages());
        });

        clientManager.sendMessage(botRole, localizationLoader.localize(Localizations.Service.REFUND_CONFIRMATION_REQUEST,
                botRole, new Localizations.Service.RefundConfirmationRequestParams(confirmationPhrase)));
    }
}
