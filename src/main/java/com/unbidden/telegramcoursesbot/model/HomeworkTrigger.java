package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework_timed_triggers")
public class HomeworkTrigger extends TimedTrigger {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_progress_id", nullable = false)
    private HomeworkProgress progress;

    @Override
    public String toString() {
        return "HomeworkTrigger(id=" + getId() + ", userId=" + getUser().getId() + ", botId=" + getBot().getId()
                + ", createdAt=" + getCreatedAt() + ", target=" + getTarget() + ", homeworkProgressId=" + progress.getId() + ")";
    }
}
