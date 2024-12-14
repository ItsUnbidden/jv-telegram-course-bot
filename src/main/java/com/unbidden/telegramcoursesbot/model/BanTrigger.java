package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "ban_timed_triggers")
@EqualsAndHashCode(callSuper = true)
public class BanTrigger extends TimedTrigger {
    private boolean isGeneral;
}
