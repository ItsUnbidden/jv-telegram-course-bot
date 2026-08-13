package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

public record SessionParamsDto(UserEntity user, Bot bot, List<Message> messages) {

}
