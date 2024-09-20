package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.dao.LocalizationLoader;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// @Service
public class HowToRevampRelationshipCourse extends AbstractCourse {
    public static final String COURSE_NAME = "how_to_revamp_relationship";
        
    public HowToRevampRelationshipCourse(
            @Autowired PaymentService paymentService,
            @Autowired LocalizationLoader textLoader,
            @Autowired TelegramBot bot,
            @Autowired CourseRepository courseRepository) {
        super(paymentService, textLoader, bot, courseRepository,
                LogManager.getLogger(HowToRevampRelationshipCourse.class), COURSE_NAME);
    }
}
