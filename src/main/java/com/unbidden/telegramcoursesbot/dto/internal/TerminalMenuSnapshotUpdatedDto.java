package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;
import java.util.Map;

import com.unbidden.telegramcoursesbot.menu.Menu.Page;
import com.unbidden.telegramcoursesbot.menu.Menu.Page.Button;
import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TerminalMenuSnapshotUpdatedDto extends MenuSnapshotUpdatedDto {
    private String beanName;
    
    private boolean terminate;

    public TerminalMenuSnapshotUpdatedDto(MenuSnapshot snapshot, Page nextPage, String beanName,
            Map<String, String> params, List<Integer> history, boolean terminate) {
        super(snapshot, nextPage, params, history);
        this.beanName = beanName;
        this.terminate = terminate;
    }

    public TerminalMenuSnapshotUpdatedDto(MenuSnapshot snapshot, Page nextPage, List<Button> buttons, List<Integer> history, 
            List<MenuSnapshotButton> snapshotButtons, String beanName, Map<String, String> params) {
        super(snapshot, nextPage, buttons, history, snapshotButtons, params);
        this.beanName = beanName;
        this.terminate = false;
    }
}
