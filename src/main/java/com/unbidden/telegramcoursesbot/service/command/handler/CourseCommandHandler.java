package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.exception.NoCoursesException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.menu.MenuKey;
import com.unbidden.telegramcoursesbot.menu.MenuOrchestrationService;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    private static final String COMMAND = "/courses";

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final CourseRepository courseRepository;

    private final MenuOrchestrationService menuService;

    private final LocalizationLoader loader;

    @Override
    @Security(authorities = {AuthorityType.BUY, AuthorityType.LAUNCH_COURSE,
            AuthorityType.LEAVE_REVIEW, AuthorityType.REFUND})
    public void handle(BotRole botRole, Message message, String[] commandParts) {
        final long numberOfCoursesInBot = courseRepository.countByBotId(botRole.getBot().getId());

        if (numberOfCoursesInBot < 1) {
            throw new NoCoursesException("There are currently no courses in bot " + botRole.getBot().getId() + ".",
                    loader.localize(Localizations.Error.NO_COURSES, botRole));
        }
        final long numberOfOwnedCourses = courseOwnershipRepository.countByUserIdAndCourseBotIdAndStatus(
                botRole.getUser().getId(), botRole.getBot().getId(), OwnershipStatus.ACTIVE);

        if (numberOfOwnedCourses == 0) {
            menuService.initiateMenu(botRole, MenuKey.COURSES, 1, Map.of());
            return;
        }
        if (numberOfCoursesInBot - numberOfOwnedCourses < 1) {
            menuService.initiateMenu(botRole, MenuKey.COURSES, 2, Map.of());
            return;
        }
        menuService.initiateMenu(botRole, MenuKey.COURSES);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public List<AuthorityType> getAuthorities() {
        return List.of(AuthorityType.BUY, AuthorityType.LAUNCH_COURSE,
                AuthorityType.LEAVE_REVIEW, AuthorityType.REFUND);
    }
}
