package com.unbidden.telegramcoursesbot.service.session;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContentSession extends Session {
    private boolean isMenuInitialized;

    private List<Message> messages;

    private boolean isSkippingConfirmation;

    public void execute(UserEntity user, Bot bot) {
        super.getFunction().accept(user, bot, messages);
    }
}
