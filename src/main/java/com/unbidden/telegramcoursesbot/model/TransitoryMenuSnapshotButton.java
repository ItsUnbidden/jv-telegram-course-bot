package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("TRANSITORY")
public class TransitoryMenuSnapshotButton extends ParameterizedMenuSnapshotButton {
    private Integer pointer;
}
