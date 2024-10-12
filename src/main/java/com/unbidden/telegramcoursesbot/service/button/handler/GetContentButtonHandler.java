package com.unbidden.telegramcoursesbot.service.button.handler;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.ContentRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.SessionService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Component
@RequiredArgsConstructor
public class GetContentButtonHandler implements ButtonHandler {
    private static final String CONTENT_UPDATE_MENU = "m_cntUpd";

    private static final String PARAM_CONTENT_ID = "${contentId}";

    private static final String SERVICE_GET_CONTENT_REQUEST = "service_get_content_request";
    private static final String SERVICE_GET_CONTENT_SUCCESS = "service_get_content_success";

    private final LocalizationLoader localizationLoader;

    private final ContentRepository contentRepository;

    private final SessionService sessionService;

    private final UserService userService;

    private final MenuService menuService;

    private final TelegramBot bot;
    
    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        final UserEntity userFromDb = userService.getUser(user.getId());
        if (!userService.isAdmin(userFromDb)) {
            return;
        }
        sessionService.createSession(user, false, m -> {
            final Long contentId = Long.parseLong(m.getText());
            final Localization success = localizationLoader.getLocalizationForUser(
                    SERVICE_GET_CONTENT_SUCCESS, m.getFrom(), PARAM_CONTENT_ID,
                    contentId);
            bot.sendMessage(SendMessage.builder()
                    .chatId(m.getFrom().getId())
                    .text(success.getData())
                    .entities(success.getEntities())
                    .build());
            List<Message> content = bot.sendContent(contentRepository.findById(contentId)
                    .orElseThrow(() -> new EntityNotFoundException("Content with id " + contentId
                    + " does not exist.")), m.getFrom());
            menuService.initiateMenu(CONTENT_UPDATE_MENU, userFromDb, contentId.toString(),
                    content.get(0).getMessageId());
        });
        final Localization request = localizationLoader.getLocalizationForUser(
                SERVICE_GET_CONTENT_REQUEST, user);

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(request.getData())
                .entities(request.getEntities())
                .build());
    }
}
