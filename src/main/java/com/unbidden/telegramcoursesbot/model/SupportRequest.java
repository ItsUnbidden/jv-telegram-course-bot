package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@Table(name = "support_requests")
@EqualsAndHashCode(callSuper = true)
public class SupportRequest extends SupportMessage {
    @OneToMany(mappedBy = "request")
    private List<SupportReply> replies;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private UserEntity staffMember;

    @Column(nullable = false)
    private SupportType supportType;

    private String tag;

    private boolean isResolved;

    public enum SupportType {
        COURSE,
        PLATFORM
    }
}
