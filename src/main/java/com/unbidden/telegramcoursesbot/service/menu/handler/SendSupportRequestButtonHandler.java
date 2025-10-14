package com.unbidden.telegramcoursesbot.service.menu.handler;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.Course;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.SupportRequest.SupportType;
import com.unbidden.telegramcoursesbot.model.content.LocalizedContent;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.service.content.ContentService;
import com.unbidden.telegramcoursesbot.service.course.CourseService;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.session.ContentSessionService;
import com.unbidden.telegramcoursesbot.service.support.SupportService;
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

    private final ContentSessionService sessionService;
    
    private final ContentService contentService;

    private final SupportService supportService;

    private final CourseService courseService;

    private final LocalizationLoader localizationLoader;

    private final ClientManager clientManager;

    @Override
    @Security(authorities = AuthorityType.ASK_SUPPORT)
    public void handle(@NonNull Bot bot, @NonNull UserEntity user, @NonNull String[] params) {
        LOGGER.info("User " + user.getId() + " is trying to get some support...");

        supportService.checkifUserIsStaffMember(user, bot);
        if (!supportService.isUserEligibleForSupport(user, bot)) {
            throw new ForbiddenOperationException("User " + user.getId() + " cannot send another "
                    + "support request without resolving previous one.", localizationLoader
                    .getLocalizationForUser(ERROR_USER_NOT_ELIGIBLE_FOR_SUPPORT, user));
        }
        sessionService.createSession(user, bot, m -> {
            final LocalizedContent content = contentService.parseAndPersistContent(bot, m);

            if (params.length > 1) {
                final Course course = courseService.getCourseById(
                        Long.parseLong(params[1]), user, bot);
    
                LOGGER.debug("User " + user.getId() + " wants support for course "
                        + course.getName() + "...", user);
                supportService.createNewSupportRequest(user, bot, SupportType.COURSE,
                        content, course.getName());
                
            } else {
                LOGGER.debug("User " + user.getId() + " wants support with "
                        + "technical issues...", user);
                supportService.createNewSupportRequest(user, bot, SupportType.PLATFORM,
                        content, null);
            }
            LOGGER.debug("Sending confirmation message...");
            clientManager.getClient(bot).sendMessage(user, localizationLoader
                    .getLocalizationForUser(SERVICE_SUPPORT_REQUEST_SENT, user));
            LOGGER.debug("Message sent.");
        });
        LOGGER.debug("Sending support content request message...");
        clientManager.getClient(bot).sendMessage(user, localizationLoader.getLocalizationForUser(
                SERVICE_SUPPORT_REQUEST_CONTENT_REQUEST, user));
        LOGGER.debug("Message sent.");
    }
}
