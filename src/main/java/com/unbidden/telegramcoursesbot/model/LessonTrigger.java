package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "lesson_timed_triggers")
@EqualsAndHashCode(callSuper = true)
public class LessonTrigger extends TimedTrigger {
    @ManyToOne
    @JoinColumn(name = "course_progress_id", nullable = false)
    private CourseProgress progress;
}
