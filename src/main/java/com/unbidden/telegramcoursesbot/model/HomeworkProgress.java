package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;

@Entity
@Data
@Table(name = "homework_progress")
public class HomeworkProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @OneToOne
    @JoinColumn(name = "content_id")
    private Content content;

    @ManyToOne
    @JoinColumn(name = "curator_id")
    private UserEntity curator;

    @Column(nullable = false)
    private Status status;

    @OneToMany()
    @JoinTable(name = "homework_progress_messages",
            joinColumns = @JoinColumn(name = "homework_progress_id"),
            inverseJoinColumns = @JoinColumn(name = "message_id"))
    private List<MessageEntity> approveMessages;

    @OneToMany()
    @JoinTable(name = "homework_progress_send_homework_messages",
            joinColumns = @JoinColumn(name = "homework_progress_id"),
            inverseJoinColumns = @JoinColumn(name = "message_id"))
    private List<MessageEntity> sendHomeworkMessages;

    public boolean isApproved() {
        return curator != null;
    }

    public enum Status {
        COMPLETED,
        DECLINED,
        AWAITS_APPROVAL,
        CONTENT_SENT,
        INITIALIZED
    }
}   
