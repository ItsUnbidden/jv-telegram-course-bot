package com.unbidden.telegramcoursesbot.dto.internal;

public record CourseMenuDto(long courseId, String localizedTitle, boolean isCompleted, boolean isRefundable, boolean isBasicReviewPresent, boolean isAdvancedReviewPresent) {

}
