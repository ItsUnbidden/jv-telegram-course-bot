package com.unbidden.telegramcoursesbot.dto;

import java.util.List;

import com.unbidden.telegramcoursesbot.model.Course.PaymentType;

import lombok.Data;

@Data
public class CourseResponseDto {
    private Long id;

    private Long titleId;

    private String localizedTitle;

    private Long descriptionId;

    private Long endId;

    private Long botId;

    private List<Long> lessonIds;

    private Integer price;

    private PaymentType paymentType;

    private Integer refundStage;

    private String externalStorePageUrl;

    private Long externalInvoiceMappingId;

    private boolean isUnderMaintenance;
    
    private boolean isHomeworkIncluded;

    private boolean isFeedbackIncluded;

    private Long version;
}
