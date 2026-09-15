package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.model.BotRole;

public record SessionParamsDto(BotRole botRole, List<Message> messages) {

}
