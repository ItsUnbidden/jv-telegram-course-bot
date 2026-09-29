package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.List;

import org.hibernate.Hibernate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "support_requests")
public class SupportRequest extends SupportMessage {
    @OneToMany(mappedBy = "request")
    @OrderBy("timestamp ASC, id ASC")
    private List<SupportReply> replies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_bot_role_id", nullable = false)
    private BotRole staffMemberBotRole;

    private boolean isResolved;

    @Override
    public String toString() {
        return "SupportRequest(id=" + getId() + ", userBotRoleId=" + getUserBotRole().getId() + ", contentId=" + getContent().getId()
                + ", timestamp=" + getTimestamp()  + ", replies=" + (Hibernate.isInitialized(replies) ? replies.stream().map(r -> r.getId()).toList() : "LAZY")
                + ", staffMemberBotRole=" + staffMemberBotRole.getId() + ", isResolved=" + isResolved + ", version=" + getVersion() + ")";
    }
}
