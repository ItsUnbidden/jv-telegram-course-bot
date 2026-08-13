package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("TERMINAL")
public class TerminalMenuSnapshotButton extends ParameterizedMenuSnapshotButton {
    private String handlerBeanName;
}
