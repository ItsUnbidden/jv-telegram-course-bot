package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public abstract class AbstractCourse implements CourseFlow {
    protected final Logger logger;

    protected final PaymentService paymentService;

    protected final LocalizationLoader localizationLoader;

    protected final TelegramBot bot;

    protected final CourseRepository courseRepository;

    private final String courseName;

    public AbstractCourse(PaymentService paymentService, LocalizationLoader localizationLoader,
            TelegramBot bot, CourseRepository courseRepository,
            Logger logger, String courseName) {
        this.paymentService = paymentService;
        this.localizationLoader = localizationLoader;
        this.bot = bot;
        this.courseRepository = courseRepository;
        this.logger = logger;
        this.courseName = courseName;
    }

    @Override
    public void initMessage(User user) {
        
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
