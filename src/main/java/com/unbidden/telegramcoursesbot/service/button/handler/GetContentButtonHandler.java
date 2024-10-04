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
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@RequiredArgsConstructor
public class GetContentButtonHandler implements ButtonHandler {
    private final LocalizationLoader localizationLoader;

    private final ContentRepository contentRepository;

    private final SessionService sessionService;

    private final UserService userService;

    private final MenuService menuService;

    private final TelegramBot bot;
    
    @Override
    public void handle(String[] params, User user) {
        final UserEntity userFromDb = userService.getUser(user.getId());
        if (!userService.isAdmin(userFromDb)) {
            return;
        }
        sessionService.createSession(user, m -> {
            final Long contentId = Long.parseLong(m.getText());
            final Localization success = localizationLoader.getLocalizationForUser(
                    "service_get_content_success", m.getFrom(), "${contentId}",
                    contentId);
            bot.sendMessage(SendMessage.builder()
                    .chatId(m.getFrom().getId())
                    .text(success.getData())
                    .entities(success.getEntities())
                    .build());
            List<Message> content = bot.sendContent(contentRepository.findById(contentId)
                    .orElseThrow(() -> new EntityNotFoundException("Content with id " + contentId
                    + " does not exist.")), m.getFrom());
            menuService.initiateMenu("m_cntUpd", userFromDb, contentId.toString(),
                    content.get(0).getMessageId());
        }, false);
        final Localization request = localizationLoader.getLocalizationForUser(
                "service_get_content_request", user);

        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(request.getData())
                .entities(request.getEntities())
                .build());
    }
}
