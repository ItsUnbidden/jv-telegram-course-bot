package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
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
@Table(name = "messages")
public class MessageEntity extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private Integer messageId;

    public MessageEntity() {

    }

    public MessageEntity(UserEntity user, Integer messageId) {
        this.user = user;
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "MessageEntity(id=" + getId() + ", user=" + user + ", messageId=" + messageId + ")";
    }
}
