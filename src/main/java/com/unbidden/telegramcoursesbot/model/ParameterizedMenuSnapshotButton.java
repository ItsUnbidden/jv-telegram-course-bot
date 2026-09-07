package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class ParameterizedMenuSnapshotButton extends MenuSnapshotButton {
    @Column(name = "param_name")
    private String paramNames;

    @Column(name = "param_value")
    private String paramValues;
}
