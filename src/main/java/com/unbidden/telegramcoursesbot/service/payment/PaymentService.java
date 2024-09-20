package com.unbidden.telegramcoursesbot.service.payment;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;

public interface PaymentService {
    boolean isAvailable(User user, String courseName);

    void sendInvoice(User user, String courseName);

    void resolvePreCheckout(PreCheckoutQuery preCheckoutQuery);

    void resolveSuccessfulPayment(Message message);

    void refund(User user, String courseName);
}
