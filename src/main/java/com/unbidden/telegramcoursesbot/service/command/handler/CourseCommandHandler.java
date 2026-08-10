package com.unbidden.telegramcoursesbot.service.command.handler;

import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.security.Security;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.CourseOwnership.OwnershipStatus;
import com.unbidden.telegramcoursesbot.repository.CourseOwnershipRepository;
import com.unbidden.telegramcoursesbot.repository.CourseRepository;
import com.unbidden.telegramcoursesbot.service.menu.MenuKey;
import com.unbidden.telegramcoursesbot.service.menu.MenuService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;

@Component
@RequiredArgsConstructor
public class CourseCommandHandler implements CommandHandler {
    private static final String COMMAND = "/courses";

    private final CourseOwnershipRepository courseOwnershipRepository;

    private final CourseRepository courseRepository;

    private final MenuService menuService;

    @Override
    @Security(authorities = {AuthorityType.BUY, AuthorityType.LAUNCH_COURSE,
            AuthorityType.LEAVE_REVIEW, AuthorityType.REFUND})
    public void handle(UserEntity user, Bot bot, Message message, String[] commandParts) {
        final long numberOfOwnedCourses = courseOwnershipRepository.countByUserIdAndCourseBotIdAndStatus(
                user.getId(), bot.getId(), OwnershipStatus.ACTIVE);

        if (numberOfOwnedCourses == 0) {
            menuService.initiateMenu(user, bot, MenuKey.AVAILABLE_COURSES);
            return;
        }
        final long numberOfCoursesInBot = courseRepository.countByBotId(bot.getId());

        if (numberOfCoursesInBot - numberOfOwnedCourses < 1) {
            menuService.initiateMenu(user, bot, MenuKey.MY_COURSES);
            return;
        }
        menuService.initiateMenu(user, bot, MenuKey.COURSES);
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
