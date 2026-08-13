package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.Map;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;

public record MenuParamsDto(UserEntity user, Bot bot, Map<String, String> params, int initialPage) {

}
