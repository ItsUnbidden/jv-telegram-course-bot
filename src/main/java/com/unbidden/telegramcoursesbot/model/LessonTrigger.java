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
@Table(name = "lesson_timed_triggers")
public class LessonTrigger extends TimedTrigger {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_progress_id", nullable = false)
    private CourseProgress progress;

    @Override
    public String toString() {
        return "LessonTrigger(id=" + getId() + ", botRoleId=" + getBotRole().getId() + ", createdAt=" + getCreatedAt()
                + ", target=" + getTarget() + ", courseProgressId=" + progress.getId() + ")";
    }
}
