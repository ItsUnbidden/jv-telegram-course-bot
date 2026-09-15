package com.unbidden.telegramcoursesbot.mapper;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.unbidden.telegramcoursesbot.config.MapperConfig;
import com.unbidden.telegramcoursesbot.dto.CourseResponseDto;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.ExternalInvoice;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.TelegramInvoice;
import com.unbidden.telegramcoursesbot.model.CourseInvoice.PaymentType;

@Mapper(config = MapperConfig.class)
public interface CourseMapper {
    @Mapping(target = "titleId", source = "course.title.id")
    @Mapping(target = "descriptionId", ignore = true)
    @Mapping(target = "endId", source = "course.endMapping.id")
    @Mapping(target = "botId", source = "course.bot.id")
    @Mapping(target = "lessonIds", source = "course.lessons")
    @Mapping(target = "externalStorePageUrl", ignore = true)
    @Mapping(target = "externalInvoiceMappingId", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "refundStage", ignore = true)
    @Mapping(target = "paymentType", ignore = true)
    CourseResponseDto toDto(Course course, String localizedTitle);

    default List<Long> mapLessons(List<Lesson> lessons) {
        return lessons.stream().map(l -> l.getId()).toList();
    }

    @AfterMapping
    default void mapInvoice(@MappingTarget CourseResponseDto dto, Course source) {
        if (source.getInvoice().getClass().equals(TelegramInvoice.class)) {
            final TelegramInvoice invoice = (TelegramInvoice)source.getInvoice();

            dto.setPaymentType(PaymentType.TELEGRAM);
            dto.setDescriptionId(invoice.getDescription().getId());
            dto.setPrice(invoice.getPrice());
            dto.setRefundStage(invoice.getRefundStage());
        } else {
            final ExternalInvoice invoice = (ExternalInvoice)source.getInvoice();

            dto.setPaymentType(PaymentType.EXTERNAL);
            dto.setExternalInvoiceMappingId(invoice.getMapping().getId());
            dto.setExternalStorePageUrl(invoice.getExternalStorePageUrl());
        }
    }
}
