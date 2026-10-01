package com.unbidden.telegramcoursesbot.dto.internal;

import java.util.List;

import com.unbidden.telegramcoursesbot.model.GeneralMenuSnapshot;
import com.unbidden.telegramcoursesbot.model.MenuSnapshotButton;

public record MenuSnapshotWithButtonsDto(GeneralMenuSnapshot snapshot, List<MenuSnapshotButton> buttons) {

}
