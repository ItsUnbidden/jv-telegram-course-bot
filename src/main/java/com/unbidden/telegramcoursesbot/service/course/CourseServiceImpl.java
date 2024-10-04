package com.unbidden.telegramcoursesbot.service.course;

import com.unbidden.telegramcoursesbot.bot.TelegramBot;
import com.unbidden.telegramcoursesbot.model.Content;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.CourseProgress;
import com.unbidden.telegramcoursesbot.model.Lesson;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.ContentRepository;
import com.unbidden.telegramcoursesbot.repository.CourseProgressRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.repository.LessonRepository;
import com.unbidden.telegramcoursesbot.service.button.menu.MenuService;
import com.unbidden.telegramcoursesbot.service.localization.Localization;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.payment.PaymentService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private static final Logger LOGGER = LogManager.getLogger(CourseServiceImpl.class);

    private final CourseRepository courseRepository;

    private final LessonRepository lessonRepository;

    private final ContentRepository contentRepository;

    private final CourseProgressRepository courseProgressRepository;

    private final PaymentService paymentService;

    private final MenuService menuService;

    private final UserService userService;

    private final HomeworkService homeworkService;

    private final LocalizationLoader localizationLoader;

    private final TelegramBot bot;

    @Override
    public void initMessage(@NonNull User user, @NonNull String courseName) {
        if (!paymentService.isAvailable(user, courseName)) {
            paymentService.sendInvoice(user, courseName);
            return;
        }
        LOGGER.info("Course " + courseName + " is available for user " + user.getId() + ".");
        final Course course = getCourseByName(courseName);
        final Optional<CourseProgress> progressOpt = courseProgressRepository
                .findByUserIdAndCourseName(user.getId(), courseName);

        CourseProgress progress;
        if (progressOpt.isPresent()) {
            progress = progressOpt.get();
            LOGGER.info("User " + user.getId() + " already has a course progress for course "
                    + courseName + ".");
        } else {
            progress = new CourseProgress();
            progress.setUser(userService.getUser(user.getId()));
            progress.setCourse(course);
            progress.setStage(0);
            progress.setCompleted(false);
            LOGGER.info("New course progress for user " + user.getId() + " and course "
                    + courseName + " has been set up. Saving...");
            courseProgressRepository.save(progress);
            LOGGER.info("Course progress saved.");
        }
        current(course, progress);
    }

    @Override
    public void next(@NonNull UserEntity user, @NonNull String courseName) {
        final CourseProgress progress = courseProgressRepository.findByUserIdAndCourseName(
                user.getId(), courseName).orElseThrow(() -> new EntityNotFoundException(
                "Course progress for course " + courseName + " and user " + user.getId()
                + " does not exist."));

        LOGGER.info("Current stage in course " + courseName + " for user " + user.getId() + " is "
                + progress.getStage() + ". Incrementing by 1...");
        progress.setStage(progress.getStage() + 1);
        courseProgressRepository.save(progress);
        LOGGER.info("Course stage incremented and progress saved.");

        if (progress.getStage() == progress.getCourse().getAmountOfLessons()) {
            LOGGER.info("User " + user.getId() + " has completed course " + courseName
                    + ". Commencing ending sequence...");
            end(user, progress);
            return;
        }
        current(progress.getCourse(), progress);
    }

    @Override
    public void current(@NonNull Course course, @NonNull CourseProgress courseProgress) {
        final Lesson lesson = lessonRepository.findByIndexAndCourseName(courseProgress.getStage(),
                course.getName()).get();
        final UserEntity user = courseProgress.getUser();

        LOGGER.info("Sending content for lesson " + lesson.getId() + " to user "
                + user.getId() + "...");
        sendContents(lesson.getStructure(), user);
        LOGGER.info("Content sent.");

        if (course.isHomeworkIncluded() && lesson.isHomeworkIncluded()) {
            LOGGER.info("Lesson " + lesson.getId() + " includes homework. "
                    + "Commencing homework sequence...");
            homeworkService.sendHomework(user, homeworkService.getHomework(
                    lesson.getHomework().getId()));
            return;
        }
                
        LOGGER.info("Lesson " + lesson.getId() + " or the course does not have any homework."
                +" Sending next lesson menu...");
        menuService.initiateMenu("m_crsNxtStg", user, course.getName());
        LOGGER.info("Next lesson menu sent.");
    }

    @Override
    public void end(@NonNull UserEntity user, @NonNull CourseProgress courseProgress) {
        courseProgress.setStage(0);
        final Localization localization = (courseProgress.isCompleted()) ? localizationLoader
                .getLocalizationForUser("course_"+ courseProgress.getCourse().getName()
                + "_end_repeat", user) : localizationLoader.getLocalizationForUser("course_"
                + courseProgress.getCourse().getName() + "_end", user);
        courseProgress.setCompleted(true);
        courseProgressRepository.save(courseProgress);
        bot.sendMessage(SendMessage.builder()
                .chatId(user.getId())
                .text(localization.getData())
                .entities(localization.getEntities())
                .build());
    }

    @Override
    @NonNull
    public Course getCourseByName(@NonNull String courseName) {
        return courseRepository.findByName(courseName).orElseThrow(() ->
                new EntityNotFoundException("Course " + courseName + " does not exist."));
    }

    @Override
    @NonNull
    public List<Course> getCourses() {
        return courseRepository.findAll();
    }

    @Override
    @NonNull
    public Course save(@NonNull Course course) {
        return courseRepository.save(course);
    }

    private void sendContents(List<Content> contents, UserEntity user) {
        for (Content content : contents) {
            bot.sendContent(contentRepository.findById(content.getId()).get(), user);
        }
    }
}
