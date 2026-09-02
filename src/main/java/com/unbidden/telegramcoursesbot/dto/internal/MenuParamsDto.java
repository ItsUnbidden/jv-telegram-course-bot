package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.Map;

import com.unbidden.telegramcoursesbot.model.BotRole;

public record MenuParamsDto(BotRole botRole, Map<String, String> params, int initialPage) {

}
