package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddMappingLocalizationButtonHandler extends AbstractButtonHandler {
    private static final String MAPPING_ID_PARAM = "mappingId";

    private final ContentOrchestrationService contentService;

    private final ContentSessionService sessionService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.CONTENT_SETTINGS)
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long mappingId = Long.parseLong(params.get(MAPPING_ID_PARAM));

        sessionService.createSession(user, bot, p -> {
            contentService.addNewLocalization(p.user(), p.bot(), mappingId, p.messages());
        });
        clientManager.getClient(bot).sendMessage(user, localizationLoader
                .localize(Localizations.Service.ADD_NEW_LOCALIZATION_REQUEST, user,
                    new Localizations.Service.AddNewLocalizationRequestParams(mappingId)));
    }
}
