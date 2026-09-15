package com.unbidden.telegramcoursesbot.menu.handler;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendRichMessage;
import org.telegram.telegrambots.meta.api.objects.richblock.InputRichBlockParagraph;
import org.telegram.telegrambots.meta.api.objects.richtext.InputRichMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.Localization;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TestButtonHandler extends AbstractButtonHandler {
    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Localization loc = loader.localize(Localizations.Service.TEST_LOC, botRole);

        clientManager.sendMessage(botRole, loc);

        try {
            clientManager.getClient(botRole.getBot()).execute(SendRichMessage.builder()
                .chatId(botRole.getUser().getId())
                .richMessage(InputRichMessage.builder()
                    .blocks(List.of(InputRichBlockParagraph.builder()
                        .text(textUtil.getRichTextFromLocalization(loc))
                        .build()))
                    .build())
                .build());
        } catch (TelegramApiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
