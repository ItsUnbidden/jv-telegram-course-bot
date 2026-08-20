package com.unbidden.telegramcoursesbot.model;

import com.unbidden.telegramcoursesbot.model.content.ContentMapping;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ExternalInvoice {
    @Column(length = 512)
    private String externalStorePageUrl;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "external_invoice_content_id")
    private ContentMapping mapping;

    public ExternalInvoice() {
        
    }

    public ExternalInvoice(String externalStorePageUrl, ContentMapping mapping) {
        this.mapping = mapping;
        this.externalStorePageUrl = externalStorePageUrl;
    }

    @Override
    public String toString() {
        return "ExternalInvoice(url=" + externalStorePageUrl + ", contentMappingId="
                + (mapping != null ? mapping.getId() : "NULL") + ")";
    }
}
