package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.exception.TelegramException;
import com.unbidden.telegramcoursesbot.model.CourseModel;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public abstract class AbstractCourse implements CourseFlow {
    protected final Logger logger;

    protected final PaymentService paymentService;

    protected final LocalizationLoader textLoader;

    protected final TelegramBot bot;

    protected final CourseRepository courseRepository;

    private final String courseName;

    public AbstractCourse(PaymentService paymentService, LocalizationLoader textLoader,
            TelegramBot bot, CourseRepository courseRepository,
            Logger logger, String courseName) {
        this.paymentService = paymentService;
        this.textLoader = textLoader;
        this.bot = bot;
        this.courseRepository = courseRepository;
        this.logger = logger;
        this.courseName = courseName;
    }

    @Override
    public void initMessage(User user) {
        final CourseModel course = courseRepository.findByName(courseName).get();

        logger.info("Checking whether user " + user.getId() + " has course "
                + courseName + "...");
        if (!paymentService.isAvailable(user, courseName)) {
            logger.info("User " + user.getId() + " does not have course " + courseName
                    + ". Sending invoice...");
            paymentService.sendInvoice(user, courseName);
            return;
        }
        logger.info("User " + user.getId() + " has course " + courseName
                + ". Sending initial message...");
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .callbackData(course.getLocFileButtonName() + "begin")
                .text(textLoader.getTextByNameForUser(
                    course.getLocFileButtonName() + "begin", user))
                .build();
        SendMessage sendMessage = SendMessage.builder()
                .chatId(user.getId())
                .text(textLoader.getTextByNameForUser(course.getLocFileMessageName()
                    + "initMessage", user))
                .replyMarkup(InlineKeyboardMarkup.builder().keyboardRow(List.of(button)).build())
                .build();
        try {
            bot.execute(sendMessage);
            logger.info("Initial message for course " + courseName
                    + " has been sent to user " + user.getId() + ".");
        } catch (TelegramApiException e) {
            throw new TelegramException("Unable to send initial message for course "
                    + courseName + " for user " + user.getId(), e);
        }
    }

    @Override
    public void start(User user) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void end(User user) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void next(User user) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void sendTask(Message message) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getCourseName() {
        return courseName;
    }
}
