package com.unbidden.telegramcoursesbot.service.course;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceSupplierImpl implements CourseServiceSupplier {
    @Autowired
    private List<CourseFlow> courses;

    @Override
    @Nullable
    public CourseFlow getService(@NonNull String courseName) {
        for (CourseFlow course : courses) {
            if (course.getCourseName().equals(courseName)) {
                return course;
            }
        }
        return null;
    }
}
