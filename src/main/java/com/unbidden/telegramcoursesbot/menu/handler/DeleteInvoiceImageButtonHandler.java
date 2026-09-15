package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;

import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteInvoiceImageButtonHandler extends AbstractButtonHandler {
    private final PaymentOrchestrationService paymentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        sessionService.createSession(botRole, p -> {
            paymentService.deleteInvoiceImage(p.botRole(), p.messages());
        }, true);

        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.INVOICE_IMAGE_DELETE_REQUEST, botRole));
    }
}
