package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "homework_timed_triggers")
@EqualsAndHashCode(callSuper = true)
public class HomeworkTrigger extends TimedTrigger {
    @ManyToOne
    @JoinColumn(name = "homework_progress_id", nullable = false)
    private HomeworkProgress progress;
}
