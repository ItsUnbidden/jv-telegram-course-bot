package com.unbidden.telegramcoursesbot.service.session;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.telegram.telegrambots.meta.api.objects.Message;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContentSession extends Session {
    private Integer lastMessageId;

    private List<Message> messages;

    public void execute() {
        super.getFunction().accept(messages);
    }
}
