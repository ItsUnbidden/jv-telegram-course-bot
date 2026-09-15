package com.unbidden.telegramcoursesbot.menu.multipage.converter;

import org.telegram.telegrambots.meta.api.objects.richtext.InputRichMessage;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.model.BotRole;

public interface MultipageListDataConverter {
    InputRichMessage convert(BotRole botRole, MultipageListData data);

    Class<? extends MultipageListData> getType();
}
