package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework_progress")
public class HomeworkProgress extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private LocalizedContent content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curator_id")
    private UserEntity curator;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_comment_content_id")
    private LocalizedContent lastComment;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime initializedAt;

    private LocalDateTime approveRequestedAt;

    private LocalDateTime finishedAt;

    @Version
    private Long version;

    public boolean isApproved() {
        return curator != null;
    }

    @Override
    public String toString() {
        return "HomeworkProgress(id=" + getId() + ", userId=" + user.getId() + ", homeworkId=" + homework.getId() + ", contentId=" + (content != null ? content.getId() : "NULL")
                + ", curatorId=" + (curator != null ? curator.getId() : "NULL") + ", lastCommentContentId=" + lastComment.getId() + ", status=" + status + ", initializedAt=" + initializedAt
                + ", approveRequestedAt=" + approveRequestedAt + ", finishedAt=" + finishedAt + ", version=" + version + ")";
    }

    public enum Status {
        COMPLETED,
        DECLINED,
        AWAITS_APPROVAL,
        CONTENT_SENT,
        INITIALIZED
    }
}   
