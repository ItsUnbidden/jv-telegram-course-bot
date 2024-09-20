package com.unbidden.telegramcoursesbot.service.course;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

public interface CourseFlow {
    void initMessage(User user);

    void start(User user);

    void next(User user);

    void end(User user);

    void sendTask(Message message);

    String getCourseName();
}
