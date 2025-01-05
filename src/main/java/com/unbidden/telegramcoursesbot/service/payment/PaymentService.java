package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;

public interface PaymentService {
    boolean isAvailable(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String courseName);

    boolean isAvailableAndGifted(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull String courseName);
    
    void sendInvoice(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String courseName);

    void resolvePreCheckout(@NonNull PreCheckoutQuery preCheckoutQuery, @NonNull Bot bot);

    void resolveSuccessfulPayment(@NonNull Message message, @NonNull Bot bot);

    PaymentDetails isRefundPossible(@NonNull UserEntity user, @NonNull Bot bot,
            @NonNull String courseName);

    void refund(@NonNull UserEntity user, @NonNull Bot bot, @NonNull String courseName);

    @NonNull
    PaymentDetails addPaymentDetails(@NonNull PaymentDetails paymentDetails);

    void deleteByCourseForUser(@NonNull String courseName, @NonNull Long userId);

    @NonNull
    List<PaymentDetails> getAllForUserAndBot(@NonNull UserEntity user, @NonNull Bot bot);

    @NonNull
    List<PaymentDetails> getAllForCourse(@NonNull Course course, @NonNull Pageable pageable);
}
