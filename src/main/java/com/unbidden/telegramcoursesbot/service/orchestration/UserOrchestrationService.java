package com.unbidden.telegramcoursesbot.service.orchestration;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.bot.RegularClient;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.service.user.UserService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserOrchestrationService {
    private static final Logger LOGGER = LogManager.getFormatterLogger(UserOrchestrationService.class);

    private final UserService userService;

    private final LocalizationLoader loader;

    private final ClientManager clientManager;

    private final ReplyKeyboardRemove keyboardRemove;

    private final EntityUtil entityUtil;

    public List<BotRole> getHomeworkReceivingUsers(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return userService.getHomeworkReceivingUsers(botId);
    }

    public BotRole initializeUserForBot(User rawUser, Long botId) {
        Assert.notNull(rawUser, "rawUser cannot be null");
        Assert.notNull(botId, "botId cannot be null");

        return userService.initializeUserForBot(rawUser, botId);
    }

    public BotRole disableUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        return userService.disableUser(botRole);
    }

    public UserEntity createDummyDirector() {
        return userService.createDummyDirector();
    }

    public BotRole setRole(BotRole botRole, Long targetId, RoleType roleType) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");

        final BotRole targetBotRole = userService.setRole(botRole, targetId, roleType);

        LOGGER.debug("Bot role " + targetBotRole.getId() + " has been updated for user "
                + targetBotRole.getUser().getId() + ". Sending confirmation...");

        clientManager.sendMessage(targetBotRole, loader.localize(Localizations.Service.ROLE_CHANGED,
                targetBotRole, new Localizations.Service.RoleChangedParams(botRole.getUser().getFullName(), roleType,
                entityUtil.getLocalizedTitle(targetBotRole, botRole))));
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.SET_ROLE_SUCCESS, botRole,
                new Localizations.Service.SetRoleSuccessParams(targetBotRole.getUser().getFullName(), roleType)));
        
        LOGGER.debug("Messages sent. Updating menus...");
        final RegularClient client = (RegularClient)clientManager.getClient(targetBotRole.getBot());

        client.removeMenuForUser(targetBotRole.getUser());
        client.setUpMenuForUserForRole(targetBotRole);
        LOGGER.info("Menus have been set up for user "+ targetId + ".");

        return targetBotRole;
    }

    public void banUserInBot(BotRole botRole, Long targetId, int hours) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        
        final BotRole targetBotRole = userService.banUserInBot(botRole, targetId, hours);
        final String title = entityUtil.getLocalizedTitle(targetBotRole, botRole);

        LOGGER.info("User " + targetId + " has been banned in bot " + targetBotRole.getBot().getId() + ".");

        LOGGER.debug("Sending confirmation messages...");
        if (hours > 0) {
            clientManager.sendMessage(targetBotRole, loader.localize(Localizations.Service.TEMPORARY_BANNED, targetBotRole,
                    new Localizations.Service.TemporaryBannedParams(botRole.getUser().getFullName(), hours, title)));
        } else {
            clientManager.sendMessage(targetBotRole, loader.localize(Localizations.Service.BANNED, targetBotRole,
                    new Localizations.Service.BannedParams(botRole.getUser().getFullName(), title)));
        }
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BAN_SUCCESS, botRole), keyboardRemove);
        LOGGER.debug("Messages sent.");
    }

    public void liftBanInBot(BotRole botRole, Long targetId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final BotRole targetBotRole = userService.liftBanInBot(botRole, targetId);

        LOGGER.info("Ban for user " + targetId + " in bot " + botRole.getBot().getId() + " has been lifted.");

        LOGGER.debug("Sending confirmation messages...");
        clientManager.sendMessage(targetBotRole, loader.localize(Localizations.Service.BAN_LIFTED,
                targetBotRole, new Localizations.Service.BanLiftedParams(botRole.getUser().getFullName(),
                entityUtil.getLocalizedTitle(targetBotRole, botRole))));
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BAN_LIFTED_SUCCESS, botRole), keyboardRemove);
        LOGGER.debug("Messages sent.");
    }

    public void liftBanInBot(BotRole targetBotRole) {
        Assert.notNull(targetBotRole, "targetBotRole cannot be null");

        final BotRole botRole = userService.liftBanInBot(targetBotRole);

        LOGGER.info("Ban for user " + botRole.getUser().getId() + " in bot " + botRole.getBot().getId() + " has been lifted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.BAN_LIFTED_AUTO, botRole));
        LOGGER.debug("Message sent.");
    }

    public void banUserGenerally(BotRole botRole, Long targetId, int hours) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final List<BotRole> botRoles = userService.banUserGenerally(botRole, targetId, hours);

        LOGGER.info("User " + targetId + " has been completely banned.");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetId + " does not have any bot roles. No messages will be sent.");
            return;
        }

        final String title = loader.localizeGeneric(Localizations.Service.ROLE_TITLE, botRoles.getFirst(), "director").getData();
        final var temporaryBanParams = new Localizations.Service.TemporaryGeneralBanParams(botRole.getUser().getFullName(), hours, title);
        final var banParams = new Localizations.Service.GeneralBanParams(botRole.getUser().getFullName(), title);

        LOGGER.debug("Sending confirmation messages...");
        if (hours > 0) {
            botRoles.forEach(br -> clientManager.sendMessageAsync(br, loader.localize(
                    Localizations.Service.TEMPORARY_GENERAL_BAN, br, temporaryBanParams)));
        } else {
            botRoles.forEach(br -> clientManager.sendMessageAsync(br, loader.localize(
                    Localizations.Service.GENERAL_BAN, br, banParams)));
        }
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GENERAL_BAN_SUCCESS, botRole));
        LOGGER.debug("Messages sent.");
    }

    public void liftGeneralBan(BotRole botRole, Long targetId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final List<BotRole> botRoles = userService.liftGeneralBan(botRole, targetId);

        LOGGER.info("General ban of user " + targetId + " has been lifted by director " + botRole.getUser().getId() + ".");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetId + " does not have any bot roles. No messages will be sent.");
            return;
        }

        final String title = loader.localizeGeneric(Localizations.Service.ROLE_TITLE, botRoles.getFirst(), "director").getData();

        LOGGER.debug("Sending confirmation messages...");
        botRoles.forEach(br -> clientManager.sendMessageAsync(br, loader.localize(Localizations.Service.GENERAL_BAN_LIFTED, br,
                new Localizations.Service.GeneralBanLiftedParams(botRole.getUser().getFullName(), title))));
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.GENERAL_BAN_LIFTED_SUCCESS, botRole), keyboardRemove);
        LOGGER.debug("Messages sent.");     
    }

    public void liftGeneralBan(BotRole targetBotRole) {
        final List<BotRole> botRoles = userService.liftGeneralBan(targetBotRole);

        LOGGER.info("General ban of user " + targetBotRole.getUser().getId() + " has been lifted.");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetBotRole.getUser().getId() + " does not have any bot roles. No messages will be sent.");
            return;
        }

        LOGGER.debug("Sending confirmation messages...");
        botRoles.forEach(br -> clientManager.sendMessageAsync(br, loader.localize(Localizations.Service.GENERAL_BAN_LIFTED_AUTO, br)));
        LOGGER.debug("Messages sent.");     
    }

    public void toggleReceiveHomework(BotRole botRole) {
        final BotRole updateBotRole = userService.toggleReceiveHomework(botRole);

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(updateBotRole, loader.localize(Localizations.Service.TOGGLE_RECEIVE_HOMEWORK, updateBotRole,
                new Localizations.Service.ToggleReceiveHomeworkParams(getStatus(updateBotRole, updateBotRole.isReceivingHomework()))));
        LOGGER.debug("Message sent.");
    }

    public void changeLanguage(BotRole botRole, String newCode) {
        LOGGER.info("Changning language for user " + botRole.getUser().getId() + " to " + newCode + "...");

        userService.changeLanguage(botRole, newCode);

        LOGGER.info("Language code for user " + botRole.getUser().getId() + " has been changed to " + newCode + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LANGUAGE_MANUALLY_SET, botRole));
        LOGGER.debug("Message sent.");
    }

    public void resetLanguageToDefault(BotRole botRole) {
        LOGGER.info("Resetting language code to default for user " + botRole.getUser().getId() + "...");

        userService.resetLanguageToDefault(botRole);

        LOGGER.info("Language code for user " + botRole.getUser().getId() + " is now automatically set.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.sendMessage(botRole, loader.localize(Localizations.Service.LANGUAGE_RESET_TO_DEFAULT, botRole));
        LOGGER.debug("Message sent.");      
    }

    private String getStatus(BotRole botRole, boolean status) {
        return status ? loader.localize(Localizations.Service.STATUS_ENABLED, botRole).getData()
                : loader.localize(Localizations.Service.STATUS_DISABLED, botRole).getData();
    }
}
