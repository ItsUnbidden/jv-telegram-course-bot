package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@DiscriminatorValue("EXTERNAL")
public class ExternalPaymentDetails extends PaymentDetails {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entitlement_id")
    private Entitlement claimedEntitlement;

    @Override
    public String toString() {
        return "ExternalPaymentDetails(id=" + getId() + ", userId=" + getUser().getId() + ", botId=" + getBot().getId()
                + ", courseId=" + getCourse().getId() + ", totalAmount=" + getTotalAmount()
                + ", timestamp=" + getTimestamp() + ", entitlementId=" + claimedEntitlement.getId()
                + ", version=" + getVersion() + ")";
    }
}
