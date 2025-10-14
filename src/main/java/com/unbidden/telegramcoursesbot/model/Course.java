package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@Entity
@Data
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "bot_id", nullable = false)
    private Bot bot;

    @OneToMany(mappedBy = "course")
    private List<Lesson> lessons;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(nullable = false)
    private Integer amountOfLessons;

    @Column(nullable = false)
    private Integer refundStage;

    private boolean isUnderMaintenance;

    private boolean isHomeworkIncluded;

    private boolean isFeedbackIncluded;

    @Override
    public String toString() {
        return "Id: " + id + "\nName: " + name + "\nNumber of lessons: " + amountOfLessons
                + "\nPrice: " + price + "\nIs homework included: " + isHomeworkIncluded
                + "\nIs feedback included: " + isFeedbackIncluded;
    }
}
