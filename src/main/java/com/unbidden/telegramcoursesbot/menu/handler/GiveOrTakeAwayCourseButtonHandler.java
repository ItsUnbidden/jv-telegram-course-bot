package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.orchestration.PaymentOrchestrationService;
import com.unbidden.telegramcoursesbot.service.session.UserOrChatRequestSessionService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.Map;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButtonRequestUser;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@RequiredArgsConstructor
public class GiveOrTakeAwayCourseButtonHandler extends AbstractButtonHandler {
    private static final String COURSE_ID_PARAM = "courseId";
    
    private final UserOrChatRequestSessionService sessionService;
    
    private final PaymentOrchestrationService paymentService;
    
    private final ContentOrchestrationService contentService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final EntityUtil entityUtil;

    @Override
    @Security(authorities = AuthorityType.GIVE_COURSE)
    public void handle(BotRole botRole, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));

        final var giveCourseSession = sessionService.createSession(botRole, getGiveCourseFunction(courseId));
        final var takeCourseSession = sessionService.createSession(botRole, getTakeCourseFunction(courseId));

        final KeyboardButtonRequestUser requestUserGiveCourse = KeyboardButtonRequestUser
                .builder()
                .userIsBot(false)
                .requestId(String.valueOf(giveCourseSession.getRequestId())).build();
        final KeyboardButtonRequestUser requestUserTakeCourse = KeyboardButtonRequestUser
                .builder()
                .userIsBot(false)
                .requestId(String.valueOf(takeCourseSession.getRequestId())).build();

        final KeyboardButton giveButton = KeyboardButton.builder()
                .requestUser(requestUserGiveCourse)
                .text(loader.localize(Localizations.Button.GIVE_COURSE, botRole).getData())
                .build();
        final KeyboardButton takeButton = KeyboardButton.builder()
                .requestUser(requestUserTakeCourse)
                .text(loader.localize(Localizations.Button.TAKE_COURSE, botRole).getData())
                .build();

        final KeyboardRow row = new KeyboardRow();

        row.add(giveButton);
        row.add(takeButton);

        final var markup = ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .build();
        
        clientManager.sendMessage(botRole, loader.localize(
                Localizations.Service.GIVE_TAKE_COURSE_CHOOSE_ACTION, botRole,
                new Localizations.Service.GiveTakeCourseChooseActionParams(
                    contentService.getLocalizedText(botRole, entityUtil.getCourseTitle(botRole, courseId)))), markup);
    }

    private Consumer<SessionParamsDto> getGiveCourseFunction(Long courseId) {
        return p -> {
            paymentService.giftCourse(p.botRole(), p.messages().getFirst().getUserShared().getUserId(), courseId);
        };
    }

    private Consumer<SessionParamsDto> getTakeCourseFunction(Long courseId) {
        return p -> {
            paymentService.takeCourse(p.botRole(), p.messages().getFirst().getUserShared().getUserId(), courseId);
        };
    }
}
