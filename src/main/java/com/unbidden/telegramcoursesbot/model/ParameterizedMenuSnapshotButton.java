package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ParameterizedMenuSnapshotButton extends MenuSnapshotButton {
    private String paramName;

    private String paramValue;
}
