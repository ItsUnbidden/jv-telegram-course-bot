package com.unbidden.telegramcoursesbot.service.payment;

import com.unbidden.telegramcoursesbot.model.PaymentDetails;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import java.util.List;
import org.springframework.lang.NonNull;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;

public interface PaymentService {
    boolean isAvailable(@NonNull User user, @NonNull String courseName);

    boolean isAvailable(@NonNull UserEntity user, @NonNull String courseName);

    boolean isAvailableAndGifted(@NonNull UserEntity user, @NonNull String courseName);
    
    void sendInvoice(@NonNull UserEntity user, @NonNull String courseName);

    void resolvePreCheckout(@NonNull PreCheckoutQuery preCheckoutQuery);

    void resolveSuccessfulPayment(@NonNull Message message);

    boolean isRefundPossible(@NonNull UserEntity user, @NonNull String courseName);

    void refund(@NonNull UserEntity user, @NonNull String courseName);

    @NonNull
    PaymentDetails addPaymentDetails(@NonNull PaymentDetails paymentDetails);

    void deleteByCourseForUser(@NonNull String courseName, @NonNull Long userId);

    @NonNull
    List<PaymentDetails> getAllForUser(@NonNull UserEntity user);
}
