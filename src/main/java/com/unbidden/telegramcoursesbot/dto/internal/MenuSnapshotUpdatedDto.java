package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;
import java.util.Map;

import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

import lombok.Data;

@Data
public abstract class MenuSnapshotUpdatedDto {
    private MenuSnapshot snapshot;
    
    private Page nextPage;
    
    private List<Button> buttons;

    private List<MenuSnapshotButton> snapshotButtons;

    private Map<String, String> params;

    public MenuSnapshotUpdatedDto(MenuSnapshot snapshot, Page nextPage, Map<String, String> params) {
        this.snapshot = snapshot;
        this.nextPage = nextPage;
        this.params = params;
    }

    public MenuSnapshotUpdatedDto(MenuSnapshot snapshot, Page nextPage, List<Button> buttons,
            List<MenuSnapshotButton> snapshotButtons, Map<String, String> params) {
        this.snapshot = snapshot;
        this.nextPage = nextPage;
        this.buttons = buttons;
        this.snapshotButtons = snapshotButtons;
        this.params = params;
    }
}
