package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@DiscriminatorValue("TELEGRAM")
public class TelegramInvoice extends CourseInvoice {
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "description_mapping_id")
    private ContentMapping description;

    private Integer price;

    private Integer refundStage;

    public TelegramInvoice() {
        
    }

    public TelegramInvoice(ContentMapping description, Integer price, Integer refundStage) {
        this.description = description;
        this.price = price;
        this.refundStage = refundStage;
    }

    @Override
    public String toString() {
        return "TelegramInvoice(descriptionId=" + (description != null ? description.getId() : "NULL")
                + ", price=" + price + ", refundStage=" + refundStage + ")";
    }
}
