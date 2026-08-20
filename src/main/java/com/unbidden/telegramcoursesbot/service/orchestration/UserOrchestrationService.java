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
import com.unbidden.telegramcoursesbot.model.Bot;
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

    public List<UserEntity> getHomeworkReceivingUsers(Bot bot) {
        return userService.getHomeworkReceivingUsers(bot);
    }

    public UserEntity initializeUserForBot(User rawUser, Bot bot) {
        return userService.initializeUserForBot(rawUser, bot);
    }

    public UserEntity createDummyDirector() {
        return userService.createDummyDirector();
    }

    public BotRole setRole(UserEntity user, Bot bot, Long targetId, RoleType roleType) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");

        final BotRole botRole = userService.setRole(user, bot, targetId, roleType);

        LOGGER.debug("Bot role " + botRole.getId() + " has been updated for user " + botRole.getUser().getId() + ". Sending confirmation...");

        clientManager.getClient(bot).sendMessage(botRole.getUser(), loader.localize(Localizations.Service.ROLE_CHANGED,
                botRole.getUser(), new Localizations.Service.RoleChangedParams(user.getFullName(), roleType,
                entityUtil.getLocalizedTitle(botRole.getUser(), bot, user))));
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.SET_ROLE_SUCCESS, user,
                new Localizations.Service.SetRoleSuccessParams(botRole.getUser().getFullName(), roleType)));
        
        LOGGER.debug("Messages sent. Updating menus...");
        final RegularClient client = (RegularClient)clientManager.getClient(bot);

        client.removeMenuForUser(botRole.getUser());
        client.setUpMenuForUserForRole(botRole.getUser(), entityUtil.getRole(roleType));
        LOGGER.info("Menus have been set up for user "+ targetId + ".");

        return botRole;
    }

    public void banUserInBot(UserEntity user, Bot bot, Long targetId, int hours) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        
        final BotRole botRole = userService.banUserInBot(user, bot, targetId, hours);
        final String title = entityUtil.getLocalizedTitle(botRole.getUser(), bot, user);

        LOGGER.info("User " + targetId + " has been banned in bot " + bot.getId() + ".");

        LOGGER.debug("Sending confirmation messages...");
        if (hours > 0) {
            clientManager.getClient(bot).sendMessage(botRole.getUser(), loader
                    .localize(Localizations.Service.TEMPORARY_BANNED, botRole.getUser(),
                        new Localizations.Service.TemporaryBannedParams(user.getFullName(), hours, title)));
        } else {
            clientManager.getClient(bot).sendMessage(botRole.getUser(), loader
                    .localize(Localizations.Service.BANNED, botRole.getUser(),
                        new Localizations.Service.BannedParams(user.getFullName(), title)));
        }
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.BAN_SUCCESS, user, keyboardRemove));
        LOGGER.debug("Messages sent.");
    }

    public void liftBanInBot(UserEntity user, Bot bot, Long targetId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final BotRole botRole = userService.liftBanInBot(user, bot, targetId);

        LOGGER.info("Ban for user " + targetId + " in bot " + bot.getId() + " has been lifted.");

        LOGGER.debug("Sending confirmation messages...");
        clientManager.getClient(bot).sendMessage(botRole.getUser(), loader
                .localize(Localizations.Service.BAN_LIFTED, botRole.getUser(),
                    new Localizations.Service.BanLiftedParams(user.getFullName(),
                    entityUtil.getLocalizedTitle(botRole.getUser(), bot, user))));
        clientManager.getClient(bot).sendMessage(user, loader
                .localize(Localizations.Service.BAN_LIFTED_SUCCESS, user), keyboardRemove);
        LOGGER.debug("Messages sent.");
    }

    public void liftBanInBot(Bot bot, UserEntity target) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");

        final BotRole botRole = userService.liftBanInBot(bot, target);

        LOGGER.info("Ban for user " + target.getId() + " in bot " + bot.getId() + " has been lifted.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(botRole.getUser(), loader
                .localize(Localizations.Service.BAN_LIFTED_AUTO, botRole.getUser()));
        LOGGER.debug("Message sent.");
    }

    public void banUserGenerally(UserEntity director, Long targetId, int hours) {
        Assert.notNull(director, "director cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final List<BotRole> botRoles = userService.banUserGenerally(director, targetId, hours);

        LOGGER.info("User " + targetId + " has been completely banned.");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetId + " does not have any bot roles. No messages will be sent.");
            return;
        }

        final UserEntity target = botRoles.getFirst().getUser();
        final String title = loader.localizeGeneric(Localizations.Service.ROLE_TITLE, target, "director").getData();
        final var temporaryBanParams = new Localizations.Service.TemporaryGeneralBanParams(director.getFullName(), hours, title);
        final var banParams = new Localizations.Service.GeneralBanParams(director.getFullName(), title);

        LOGGER.debug("Sending confirmation messages...");
        if (hours > 0) {
            botRoles.forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessageAsync(target, loader.localize(
                    Localizations.Service.TEMPORARY_GENERAL_BAN, target, temporaryBanParams)));
        } else {
            botRoles.forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessageAsync(target, loader.localize(
                    Localizations.Service.GENERAL_BAN, target, banParams)));
        }
        clientManager.getBotLordClient().sendMessage(director, loader.localize(
                Localizations.Service.GENERAL_BAN_SUCCESS, director));
        LOGGER.debug("Messages sent.");
    }

    public void liftGeneralBan(UserEntity director, Long targetId) {
        final List<BotRole> botRoles = userService.liftGeneralBan(director, targetId);

        LOGGER.info("General ban of user " + targetId + " has been lifted by director " + director.getId() + ".");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetId + " does not have any bot roles. No messages will be sent.");
            return;
        }

        final UserEntity target = botRoles.getFirst().getUser();
        final String title = loader.localizeGeneric(Localizations.Service.ROLE_TITLE, target, "director").getData();

        LOGGER.debug("Sending confirmation messages...");
        botRoles.forEach(br -> clientManager.getClient(br.getBot())
                .sendMessageAsync(target, loader
                .localize(Localizations.Service.GENERAL_BAN_LIFTED, target,
                    new Localizations.Service.GeneralBanLiftedParams(director.getFullName(), title))));
        clientManager.getBotLordClient().sendMessage(director, loader.localize(
                Localizations.Service.GENERAL_BAN_LIFTED_SUCCESS, director),
                keyboardRemove);
        LOGGER.debug("Messages sent.");     
    }

    public void liftGeneralBan(Long targetId) {
        final List<BotRole> botRoles = userService.liftGeneralBan(targetId);

        LOGGER.info("General ban of user " + targetId + " has been lifted.");
        if (botRoles.isEmpty()) {
            LOGGER.warn("User " + targetId + " does not have any bot roles. No messages will be sent.");
            return;
        }

        final UserEntity target = botRoles.getFirst().getUser();

        LOGGER.debug("Sending confirmation messages...");
        botRoles.forEach(br -> clientManager.getClient(br.getBot())
                .sendMessageAsync(target, loader
                .localize(Localizations.Service.GENERAL_BAN_LIFTED_AUTO, target)));
        LOGGER.debug("Messages sent.");     
    }

    public UserEntity updateUser(User user) {
        return userService.updateUser(user);
    }

    public void toggleReceiveHomework(UserEntity user, Bot bot) {
        final BotRole botRole = userService.toggleReceiveHomework(user, bot);

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader.localize(Localizations.Service.TOGGLE_RECEIVE_HOMEWORK, user,
                new Localizations.Service.ToggleReceiveHomeworkParams(getStatus(user, botRole.isReceivingHomework()))));
        LOGGER.debug("Message sent.");
    }

    public void changeLanguage(UserEntity user, Bot bot, String newCode) {
        LOGGER.info("Changning language for user " + user.getId() + " to " + newCode + "...");

        userService.changeLanguage(user, newCode);

        LOGGER.info("Language code for user " + user.getId() + " has been changed to " + newCode + ".");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader
                .localize(Localizations.Service.LANGUAGE_MANUALLY_SET, user));
        LOGGER.debug("Message sent.");
    }

    public void resetLanguageToDefault(UserEntity user, Bot bot) {
        LOGGER.info("Resetting language code to default for user " + user.getId() + "...");

        userService.resetLanguageToDefault(user);

        LOGGER.info("Language code for user " + user.getId() + " is now automatically set.");

        LOGGER.debug("Sending confirmation message...");
        clientManager.getClient(bot).sendMessage(user, loader
                .localize(Localizations.Service.LANGUAGE_RESET_TO_DEFAULT, user));
        LOGGER.debug("Message sent.");      
    }

    private String getStatus(UserEntity user, boolean status) {
        return status ? loader.localize(Localizations.Service.STATUS_ENABLED, user).getData()
                : loader.localize(Localizations.Service.STATUS_DISABLED, user).getData();
    }
}
