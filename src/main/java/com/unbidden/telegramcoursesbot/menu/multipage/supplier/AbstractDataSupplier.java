package com.unbidden.telegramcoursesbot.menu.multipage.supplier;

import java.util.Map;

import com.unbidden.telegramcoursesbot.dto.internal.MultipageListData;
import com.unbidden.telegramcoursesbot.menu.NamedSpringBean;
import com.unbidden.telegramcoursesbot.model.BotRole;

public abstract class AbstractDataSupplier extends NamedSpringBean {
    public abstract MultipageListData fetchData(BotRole botRole, int page, Map<String, String> params);
}
