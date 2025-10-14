package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "support_replies")
@EqualsAndHashCode(callSuper = true)
public class SupportReply extends SupportMessage {
    @OneToOne
    @JoinColumn(name = "reply_id")
    private SupportReply reply;

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    private SupportRequest request;

    @Column(nullable = false)
    private ReplySide replySide;

    public enum ReplySide {
        SUPPORT,
        CUSTOMER
    }
}
