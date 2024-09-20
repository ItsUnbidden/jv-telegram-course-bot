package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

import lombok.Data;

@Entity
@Data
@Table(name = "courses")
public class CourseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private SequenceOption sequenceOption;

    @OneToMany(mappedBy = "course")
    private List<Lesson> lessons;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(nullable = false)
    private Integer amountOfLessons;

    private boolean isHomeworkIncluded;

    private boolean isFeedbackIncluded;

    public String getLocFileMessageName() {
        return "message_" + name + "_";
    }

    public String getLocFileButtonName() {
        return "button_" + name + "_";
    }

    public String getLocFilePhotoName() {
        return "photo_" + name + "_";
    }

    public String getLocFileInvoiceName() {
        return "invoice_" + name + "_";
    }

    public String getLocFileErrorName() {
        return "error_" + name + "_";
    }

    public String getLocFileCourseName() {
        return "course_" + name;
    }

    public enum SequenceOption {
        AFTER_HOMEWORK_DONE,
        TIMED
    }
}
