package com.unbidden.telegramcoursesbot.menu.handler;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.config.properties.LocalizationsProperties;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetLocalizationFileTemplatesButtonHandler extends AbstractButtonHandler {
    private static final Logger LOGGER = LogManager.getFormatterLogger(GetLocalizationFileTemplatesButtonHandler.class);

    private final ClientManager clientManager;

    private final TextUtil textUtil;

    private final LocalizationsProperties localizationsProperties;

    @Override
    @Security(authorities = AuthorityType.MAINTENANCE, isBotLordOnly = true)
    public void handle(BotRole botRole, Map<String, String> params) {
        LOGGER.info("Collecting localization info...");
        final Map<String, Map<String, List<String>>> localizationsInfo = Localizations.getInfo();

        LOGGER.info("Generating localization templates...");
        final List<InputMedia> medias = new ArrayList<>();

        for (final var entry : localizationsInfo.entrySet()) {
            medias.add(new InputMediaDocument(new ByteArrayInputStream(textUtil.generateLocalizationTemplate(entry.getValue())
                    .getBytes(StandardCharsets.UTF_8)), entry.getKey() + localizationsProperties.format()));
        }
        LOGGER.info("Sending template files...");
        clientManager.getBotLordClient().executeAsync(SendMediaGroup.builder()
                .chatId(botRole.getUser().getId())
                .medias(medias)
                .build());
        LOGGER.info("Template files have been sent.");
    }
}
