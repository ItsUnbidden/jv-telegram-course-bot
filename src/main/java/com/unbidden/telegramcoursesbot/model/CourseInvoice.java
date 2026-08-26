package com.unbidden.telegramcoursesbot.model;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
@DiscriminatorColumn(name = "payment_type")
public abstract class CourseInvoice {
    public static enum PaymentType {
        EXTERNAL,
        TELEGRAM
    }
}
