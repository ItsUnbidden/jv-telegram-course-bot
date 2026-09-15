package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;

import com.unbidden.telegramcoursesbot.model.MenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

public record MenuSnapshotCreatedDto(MenuSnapshot snapshot, List<MenuSnapshotButton> buttons) {

}
