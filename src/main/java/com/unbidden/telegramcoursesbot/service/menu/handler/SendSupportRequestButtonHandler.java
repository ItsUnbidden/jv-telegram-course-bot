package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.CustomTelegramClient;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.SupportRequest.SupportType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendSupportRequestButtonHandler implements ButtonHandler {
    private static final Logger LOGGER = LogManager.getLogger(
            SendSupportRequestButtonHandler.class);
            
    private static final String SERVICE_SUPPORT_REQUEST_SENT = "service_support_request_sent";

    private static final String SERVICE_SUPPORT_REQUEST_CONTENT_REQUEST =
            "service_support_request_content_request";

    private static final String ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT =
            "error_user_not_eligible_for_support";
    private static final String ERROR_SUPPORT_STAFF_REQUEST = "error_support_staff_request";

    private final ContentSessionService sessionService;
    
    private final ContentService contentService;

    private final SupportService supportService;

    private final UserService userService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final CustomTelegramClient client;

    @Override
    public void handle(@NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.info("User " + user.getId() + " is trying to get some support...");

        final Set<UserEntity> uneligibleUsers = new HashSet<>();
        uneligibleUsers.addAll(userService.getSupport());
        uneligibleUsers.add(userService.getDiretor());
        uneligibleUsers.add(userService.getCreator());
        if (uneligibleUsers.contains(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " is a part of the "
                    + "staff, they are uneligible for support", localizationLoader
                    .getLocalizationForUser(ERROR_SUPPORT_STAFF_REQUEST, user));
        }
        if (!supportService.isUserEligibleForSupport(user)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving previous one.", localizationLoader
                    .getLocalizationForUser(ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }
        sessionService.createSession(user, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(m);

            if (params.length > 1) {
                final Course course = courseService.getCourseById(Long.parseLong(params[1]), user);
    
                LOGGER.debug("User " + user.getId() + " wants support for course "
                        + course.getName() + "...", user);
                supportService.createNewSupportRequest(user, SupportType.COURSE,
                        content, course.getName());
                
            } else {
                LOGGER.debug("User " + user.getId() + " wants support with "
                        + "technical issues...", user);
                supportService.createNewSupportRequest(user, SupportType.PLATFORM,
                        content, null);
            }
            LOGGER.debug("Sending confirmation message...");
            client.sendMessage(user, localizationLoader.getLocalizationForUser(
                    SERVICE_SUPPORT_REQUEST_SENT, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending support content request message...");
        client.sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REQUEST_CONTENT_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
