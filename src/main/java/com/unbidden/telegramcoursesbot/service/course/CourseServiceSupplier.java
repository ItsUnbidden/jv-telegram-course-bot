package com.unbidden.telegramcoursesbot.service.course;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface CourseServiceSupplier {
    @Nullable
    CourseFlow getService(@NonNull String courseName);
}
