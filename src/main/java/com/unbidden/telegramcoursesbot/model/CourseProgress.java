package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_progress")
public class CourseProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Integer stage;

    @Column(nullable = false)
    private LocalDateTime firstTimeStartedAt;

    private LocalDateTime firstTimeFinishedAt;

    @Column(nullable = false)
    private Integer numberOfTimesCompleted;

    @Version
    private Long version;

    @Override
    public String toString() {
        return "CourseProgress(id=" + getId() + ", userId=" + user.getId() + ", courseId=" + course.getId() + ", stage=" + stage + ", firstTimeStartedAt="
                + firstTimeStartedAt + ", firstTimeFinishedAt=" + firstTimeFinishedAt
                + ", numberOfTimesCompleted=" + numberOfTimesCompleted + ", version=" + version + ")";
    }
}
