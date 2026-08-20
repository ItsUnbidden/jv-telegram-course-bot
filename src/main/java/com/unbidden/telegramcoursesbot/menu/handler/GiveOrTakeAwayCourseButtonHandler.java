package com.unbidden.telegramcoursesbot.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.dto.internal.SessionParamsDto;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
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
    public void handle(UserEntity user, Bot bot, Map<String, String> params) {
        final Long courseId = Long.parseLong(params.get(COURSE_ID_PARAM));

        final KeyboardButtonRequestUser requestUserGiveCourse = KeyboardButtonRequestUser
                .builder()
                .userIsBot(false)
                .requestId(String.valueOf(sessionService.createSession(user, bot,
                    getGiveCourseFunction(courseId)))).build();
        final KeyboardButtonRequestUser requestUserTakeCourse = KeyboardButtonRequestUser
                .builder()
                .userIsBot(false)
                .requestId(String.valueOf(sessionService.createSession(user, bot,
                    getTakeCourseFunction(courseId)))).build();

        final KeyboardButton giveButton = KeyboardButton.builder()
                .requestUser(requestUserGiveCourse)
                .text(loader.localize(Localizations.Button.GIVE_COURSE, user).getData())
                .build();
        final KeyboardButton takeButton = KeyboardButton.builder()
                .requestUser(requestUserTakeCourse)
                .text(loader.localize(Localizations.Button.TAKE_COURSE, user).getData())
                .build();

        final KeyboardRow row = new KeyboardRow();

        row.add(giveButton);
        row.add(takeButton);

        final var markup = ReplyKeyboardMarkup.builder()
                .keyboardRow(row)
                .resizeKeyboard(true)
                .build();
        
        clientManager.getClient(bot).sendMessage(user, loader.localize(
                Localizations.Service.GIVE_TAKE_COURSE_CHOOSE_ACTION, user,
                new Localizations.Service.GiveTakeCourseChooseActionParams(
                    contentService.getLocalizedText(user, bot, entityUtil.getCourseTitle(user, bot, courseId)))), markup);
    }

    private Consumer<SessionParamsDto> getGiveCourseFunction(Long courseId) {
        return p -> {
            paymentService.giftCourse(p.user(), p.bot(), p.messages().getFirst().getUserShared().getUserId(), courseId);
        };
    }

    private Consumer<SessionParamsDto> getTakeCourseFunction(Long courseId) {
        return p -> {
            paymentService.takeCourse(p.user(), p.bot(), p.messages().getFirst().getUserShared().getUserId(), courseId);
        };
    }
}
