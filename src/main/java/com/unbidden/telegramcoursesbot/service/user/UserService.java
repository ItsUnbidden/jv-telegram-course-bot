package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;
import com.unbidden.telegramcoursesbot.util.TextUtil;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger LOGGER = LogManager.getLogger(UserService.class);

    private final UserRepository userRepository;

    private final BotRoleRepository botRoleRepository;

    private final LocalizationLoader localizationLoader;

    private final TimingService timingService;

    private final EntityUtil entityUtil;

    private final TextUtil textUtil;

    @Value("${telegram.bot.authorization.director.id}")
    private Long directorId;

    @Transactional(readOnly = true)
    public List<UserEntity> getHomeworkReceivingUsers(Bot bot) {
        return userRepository.findByReceivingHomeworkInBot(bot.getId());
    }

    @Transactional
    public UserEntity initializeUserForBot(User rawUser, Bot bot) {
        Assert.notNull(rawUser, "rawUser cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final UserEntity user = updateUser(rawUser);
        final Optional<BotRole> botRoleOpt = botRoleRepository.findByBotIdAndUserId(bot.getId(), user.getId());

        if (botRoleOpt.isPresent()) {
            if (botRoleOpt.get().isDisabled()) {
                LOGGER.info("User " + user.getId() + " will be reactivated in bot " + bot.getId() + ".");
                botRoleOpt.get().setDisabled(false);
            }
            return user;
        }
        
        LOGGER.debug("New user " + user.getId() + " is being registered in bot " + bot.getId() + "...");
        botRoleRepository.save(new BotRole(bot, user, entityUtil.getRole(RoleType.USER), false));
        LOGGER.debug("User " + user.getId() + " has been registered in bot " + bot.getId() + ".");
        
        return user;
    }

    @Transactional
    public BotRole disableUser(UserEntity user, Bot bot) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");

        final BotRole botRole = botRoleRepository.findByBotIdAndUserId(bot.getId(), user.getId()).orElseThrow(() ->
                new EntityNotFoundException("Bot role for user " + user.getId() + " in bot " + bot.getId()
                + " has not been found while trying to disable it. This is likely a bug.",
                localizationLoader.localize(Localizations.Error.BOT_ROLE_NOT_FOUND, user)));

        LOGGER.info("User " + user.getId() + " will be disabled in bot " + bot.getId() + ".");
        
        botRole.setDisabled(true);

        return botRole;
    }

    @Transactional
    public UserEntity createDummyDirector() {
        final Optional<UserEntity> potentialDirector = userRepository.findById(directorId);

        if (potentialDirector.isPresent()) {
            return potentialDirector.get();
        }
        LOGGER.info("Creating dummy director for the time being...");
        final UserEntity director = new UserEntity();

        director.setId(directorId);
        director.setFirstName("director");
        director.setLanguageCode("en");
        director.setBanned(false);

        userRepository.save(director);
        LOGGER.info("Temporary director dummy created with id " + directorId);

        return director;
    }

    @Transactional
    public BotRole setRole(UserEntity user, Bot bot, Long targetId, RoleType roleType) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");

        final BotRole botRole = entityUtil.getBotRole(user, bot, targetId);

        if (roleType == RoleType.DIRECTOR || roleType == RoleType.CREATOR) {
            throw new ForbiddenOperationException("Director and Creator roles are predefined",
                    localizationLoader.localize(Error.PREDEFINED_CHANGE_ROLES,
                    user));
        }
        if (roleType == RoleType.BANNED) {
            throw new ForbiddenOperationException("Bans must be given through different means",
                    localizationLoader.localize(Error.CANNOT_SET_BANNED_ROLE,
                    user));
        }
        if (roleType == botRole.getRole().getType()) {
            throw new ForbiddenOperationException("Role is the same",
                    localizationLoader.localize(Error.SAME_ROLE, user));
        }
        if (botRole.getRole().getType() == RoleType.DIRECTOR) {
            throw new ForbiddenOperationException("Director's role is permanent",
                    localizationLoader.localize(Error.DIRECTOR_CHANGE_ROLE, user));
        }
        if (botRole.getRole().getType() == RoleType.CREATOR) {
            throw new ForbiddenOperationException("Creator's role is permanent",
                    localizationLoader.localize(Error.CREATOR_CHANGE_ROLE, user));
        }
        if (user.getId().equals(targetId)) {
            throw new ForbiddenOperationException("User cannot change their own role",
                    localizationLoader.localize(Error.SELF_CHANGE_ROLE, user));
        }
        LOGGER.info("Changing the role of user " + targetId + " in bot " + bot.getId() + " to " + roleType + "...");

        botRole.setRole(entityUtil.getRole(roleType));

        return botRole;
    }

    @Transactional
    public BotRole banUserInBot(UserEntity user, Bot bot, Long targetId, int hours) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        
        final BotRole botRole = entityUtil.getBotRole(user, bot, targetId);

        if (botRole.getRole().getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .localize(Error.USER_ALREADY_BANNED, user));
        }
        if (botRole.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .localize(Error.DIRECTOR_BAN, user));
        }
        if (botRole.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator cannot be banned by a bot "
                    + "specific ban", localizationLoader.localize(
                    Error.CREATOR_BAN, user));
        }
        if (user.getId().equals(targetId)) {
            throw new ForbiddenOperationException("User cannot ban themselves", localizationLoader
                    .localize(Error.SELF_BAN, user));
        }
        LOGGER.info("Banning user " + targetId + " in bot " + bot.getId() + "...");
        
        botRole.setRole(entityUtil.getRole(RoleType.BANNED));

        if (hours > 0) {
            LOGGER.debug("Ban is temporary. Creating trigger...");
            timingService.createBanTrigger(botRole.getUser(), bot, hours, false);
        }

        return botRole;
    }

    @Transactional
    public BotRole liftBanInBot(Bot bot, UserEntity target) {
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(target, "target cannot be null");

        return liftBanInBot0(null, bot, target);
    }

    @Transactional
    public BotRole liftBanInBot(UserEntity user, Bot bot, Long targetId) {
        Assert.notNull(user, "user cannot be null");
        Assert.notNull(bot, "bot cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return liftBanInBot0(user, bot, entityUtil.getUser(targetId, user.getLanguageCode()));
    }

    @Transactional
    public List<BotRole> banUserGenerally(UserEntity director, Long targetId, int hours) {
        Assert.notNull(director, "director cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final UserEntity target = entityUtil.getUser(targetId, director.getLanguageCode());

        if (target.isBanned()) {
            throw new ForbiddenOperationException("User " + targetId + " is already banned.", localizationLoader
                    .localize(Error.USER_ALREADY_BANNED, director));
        }
        if (targetId.equals(directorId)) {
            throw new ForbiddenOperationException("Director cannot be banned.", localizationLoader
                    .localize(Error.DIRECTOR_BAN, target));
        }
        LOGGER.debug("Director " + director.getId() + " wants to give a general ban to user " + targetId + ".");

        if (hours > 0) {
            LOGGER.debug("General ban is temporary. Creating trigger...");
            timingService.createBanTrigger(target, entityUtil.getBotLord(), hours, true);
        }

        target.setBanned(true);

        return botRoleRepository.findByUserId(targetId);
    }

    @Transactional
    public List<BotRole> liftGeneralBan(UserEntity director, Long targetId) {
        Assert.notNull(director, "director cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return liftGeneralBan0(director, targetId);
    }

    @Transactional
    public List<BotRole> liftGeneralBan(Long targetId) {
        Assert.notNull(targetId, "targetId cannot be null");

        return liftGeneralBan0(null, targetId);
    }

    /**
     * Creates or updates user entity if anything's changed. Returns the user.
     */
    @Transactional
    public UserEntity updateUser(User user) {
        LOGGER.trace("Checking if user " + user.getId() + "' data is up to date...");

        final UserEntity userFromDb;
        final Optional<UserEntity> potentialUser = userRepository.findById(user.getId());

        if (potentialUser.isEmpty()) {
            userFromDb = new UserEntity(user);
        } else {
            userFromDb = potentialUser.get();
        }

        userFromDb.setFirstName(user.getFirstName());
        userFromDb.setLastName(user.getLastName());
        userFromDb.setUsername(user.getUserName());
        
        if (!userFromDb.isLanguageManuallySet()) {
            if (user.getLanguageCode() != null) {
                userFromDb.setLanguageCode(user.getLanguageCode());
            } else {
                userFromDb.setLanguageCode(textUtil.getLanguagePriority().getFirst());
                LOGGER.debug("Language code is unavailable. Setting to " + userFromDb.getLanguageCode() + "...");
            }
        }
        
        return userRepository.save(userFromDb);
    }

    @Transactional
    public BotRole toggleReceiveHomework(UserEntity user, Bot bot) {
        final BotRole botRole = entityUtil.getBotRole(user, bot);
        
        botRole.setReceivingHomework(!botRole.isReceivingHomework());

        LOGGER.info("Receive homework for user " + user.getId() + " in bot " + bot.getId()
                + " is now " + getStatus(botRole.isReceivingHomework()) + ".");

        return botRoleRepository.save(botRole);
    }

    @Transactional
    public UserEntity changeLanguage(UserEntity user, String newCode) {
        final UserEntity userFromDb = entityUtil.getUser(user.getId(), user.getLanguageCode());

        LOGGER.info("Changning language for user " + userFromDb.getId() + " to " + newCode + "...");

        userFromDb.setLanguageCode(newCode);
        userFromDb.setLanguageManuallySet(true);

        return userFromDb;
    }

    @Transactional
    public UserEntity resetLanguageToDefault(UserEntity user) {
        final UserEntity userFromDb = entityUtil.getUser(user.getId(), user.getLanguageCode());

        LOGGER.info("Resetting language code to default for user " + userFromDb.getId() + "...");

        userFromDb.setLanguageManuallySet(false);

        return userFromDb;
    }

    private String getStatus(boolean status) {
        return status ? "ENABLED" : "DISABLED";
    }

    private BotRole liftBanInBot0(UserEntity user, Bot bot, UserEntity target) {
        final BotRole botRole = entityUtil.getBotRole(user, bot, target.getId());

        if (botRole.getRole().getType() != RoleType.BANNED) {
            if (user == null) {
                LOGGER.debug("User " + target.getId() + " does not have a ban. The timed trigger request will be ignored.");
                return botRole;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .localize(Error.USER_IS_NOT_BANNED, user));
            }
        }
        LOGGER.info("User " + target.getId() + " is banned. Removing ban...");

        botRole.setRole(entityUtil.getRole(RoleType.USER));

        if (user != null) {
            final int triggersRemoved = timingService.removeBanTriggerIfPresent(target.getId(), bot.getId());
            
            LOGGER.debug(triggersRemoved + " ban triggers have been removed for user " + target.getId() + " in bot " + bot.getId() + ".");
        }

        return botRole;
    }

    private List<BotRole> liftGeneralBan0(UserEntity director, Long targetId) {
        final UserEntity target = entityUtil.getUser(targetId, entityUtil.getDiretor().getLanguageCode());

        if (!target.isBanned()) {
            if (director == null) {
                LOGGER.debug("User " + target.getId() + " does not have a ban. The timed trigger request will be ignored.");
                return List.of();
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .localize(Error.USER_IS_NOT_BANNED, director));
            }
        }
        LOGGER.debug("User " + target.getId() + " has a general ban. Removing ban...");

        target.setBanned(false);

        if (director != null) {        
            final int triggersRemoved = timingService.removeBanTriggerIfPresent(target.getId(), entityUtil.getBotLord().getId());
            
            LOGGER.debug(triggersRemoved + " general ban triggers have been removed for user " + target.getId());
        }

        return botRoleRepository.findByUserId(targetId);
    }
}
