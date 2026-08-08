package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ban_timed_triggers")
public class BanTrigger extends TimedTrigger {
    private boolean isGeneral;

    @Override
    public String toString() {
        return "BanTrigger(id=" + getId() + ", userId=" + getUser().getId() + ", botId=" + getBot().getId()
                + ", createdAt=" + getCreatedAt() + ", target=" + getTarget() + ", isGeneral=" + isGeneral + ")";
    }
}
