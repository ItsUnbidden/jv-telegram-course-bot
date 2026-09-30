package com.unbidden.telegramcoursesbot.service.command.handler;

import java.util.List;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.orchestration.SupportOrchestrationService;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class SupportFeedbackCommandHandler implements CommandHandler {
    private static final String COMMAND = "/supportfeedback";

    private final MenuOrchestrationService menuService;

    private final SupportOrchestrationService supportService;
    
    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ANSWER_SUPPORT)
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        if (supportService.countRequestsForStaffMember(botRole) < 1) {
            clientManager.sendMessage(botRole, loader.localize(Localizations.Service.NO_SUPPORT_REQUESTS_FOR_STAFF, botRole));
            return;
        }
        menuService.initiateMenu(botRole, MenuKey.SUPPORT_FEEDBACK);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.ANSWER_SUPPORT);
    }
}
