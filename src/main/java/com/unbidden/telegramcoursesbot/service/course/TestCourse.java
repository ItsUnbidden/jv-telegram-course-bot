package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TestCourse extends AbstractCourse {
    private static final String COURSE_NAME = "test_course";

    public TestCourse(
            @Autowired PaymentService paymentService,
            @Autowired LocalizationLoader localizationLoader,
            @Autowired TelegramBot bot,
            @Autowired CourseRepository courseRepository) {
        super(paymentService, localizationLoader, bot, courseRepository,
                LogManager.getLogger(TestCourse.class), COURSE_NAME);
    }
}
