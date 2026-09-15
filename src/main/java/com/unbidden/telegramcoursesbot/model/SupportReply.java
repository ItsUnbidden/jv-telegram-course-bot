package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "support_replies")
public class SupportReply extends SupportMessage {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private SupportReply reply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private SupportRequest request;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ReplySide replySide;

    public enum ReplySide {
        SUPPORT,
        CUSTOMER
    }

    @Override
    public String toString() {
        return "SupportReply(id=" + getId() + ", userId=" + getUser().getId() + ", contentId=" + getContent().getId()
                + ", timestamp=" + getTimestamp() + ", botId=" + getBot().getId() + ", replyId=" + (reply != null ? reply.getId() : "NULL")
                + ", requestId=" + request.getId() + ", replySide=" + replySide + ", version=" + getVersion() + ")";
    }
}
