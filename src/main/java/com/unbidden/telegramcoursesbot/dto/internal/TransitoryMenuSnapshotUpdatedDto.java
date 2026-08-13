package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;
import java.util.Map;

import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

public class TransitoryMenuSnapshotUpdatedDto extends MenuSnapshotUpdatedDto {
    public TransitoryMenuSnapshotUpdatedDto(MenuSnapshot snapshot, Page nextPage, List<Button> buttons,
            List<MenuSnapshotButton> snapshotButtons, Map<String, String> params) {
        super(snapshot, nextPage, buttons, snapshotButtons, params);
    }
}
