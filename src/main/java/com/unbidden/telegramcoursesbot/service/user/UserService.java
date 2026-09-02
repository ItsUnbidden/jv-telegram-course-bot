package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
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
    public List<BotRole> getHomeworkReceivingUsers(Long botId) {
        Assert.notNull(botId, "botId cannot be null");

        return botRoleRepository.findByReceivingHomeworkInBot(botId);
    }

    @Transactional
    public BotRole initializeUserForBot(User rawUser, Long botId) {
        Assert.notNull(rawUser, "rawUser cannot be null");
        Assert.notNull(botId, "botId cannot be null");

        final Optional<BotRole> botRoleOpt = botRoleRepository.findByBotIdAndUserId(botId, rawUser.getId());

        if (botRoleOpt.isPresent()) {
            if (botRoleOpt.get().isDisabled()) {
                LOGGER.info("User " + rawUser.getId() + " will be reactivated in bot " + botId + ".");
                botRoleOpt.get().setDisabled(false);
            }
            botRoleOpt.get().setUser(updateUser(botRoleOpt.get().getUser()));
            return botRoleOpt.get();
        }
        
        LOGGER.debug("New user " + rawUser.getId() + " is being registered in bot " + botId + "...");
        final BotRole newRole = botRoleRepository.save(new BotRole(entityUtil.getBotReference(botId),
                updateUser(new UserEntity(rawUser)), entityUtil.getRole(RoleType.USER), false));
                
        LOGGER.debug("User " + rawUser.getId() + " has been registered in bot " + botId + ".");
        
        return newRole;
    }

    @Transactional
    public BotRole disableUser(BotRole botRole) {
        Assert.notNull(botRole, "botRole cannot be null");

        final BotRole botRoleFromDb = botRoleRepository.findByBotIdAndUserId(botRole.getBot().getId(), botRole.getUser().getId()).orElseThrow(() ->
                new EntityNotFoundException("Bot role for user " + botRole.getUser().getId() + " in bot " + botRole.getBot().getId()
                + " has not been found while trying to disable it. This is likely a bug.",
                localizationLoader.localize(Localizations.Error.BOT_ROLE_NOT_FOUND, botRole)));

        LOGGER.info("User " + botRoleFromDb.getUser().getId() + " will be disabled in bot " + botRoleFromDb.getBot().getId() + ".");
        
        botRoleFromDb.setDisabled(true);

        return botRoleFromDb;
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
    public BotRole setRole(BotRole botRole, Long targetId, RoleType roleType) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        Assert.notNull(roleType, "roleType cannot be null");

        final BotRole botRoleFromDb = entityUtil.getActiveBotRole(botRole, targetId);

        if (roleType == RoleType.DIRECTOR || roleType == RoleType.CREATOR) {
            throw new ForbiddenOperationException("Director and Creator roles are predefined",
                    localizationLoader.localize(Error.PREDEFINED_CHANGE_ROLES, botRole));
        }
        if (roleType == RoleType.BANNED) {
            throw new ForbiddenOperationException("Bans must be given through different means",
                    localizationLoader.localize(Error.CANNOT_SET_BANNED_ROLE, botRole));
        }
        if (roleType == botRoleFromDb.getRole().getType()) {
            throw new ForbiddenOperationException("Role is the same",
                    localizationLoader.localize(Error.SAME_ROLE, botRole));
        }
        if (botRoleFromDb.getRole().getType() == RoleType.DIRECTOR) {
            throw new ForbiddenOperationException("Director's role is permanent",
                    localizationLoader.localize(Error.DIRECTOR_CHANGE_ROLE, botRole));
        }
        if (botRoleFromDb.getRole().getType() == RoleType.CREATOR) {
            throw new ForbiddenOperationException("Creator's role is permanent",
                    localizationLoader.localize(Error.CREATOR_CHANGE_ROLE, botRole));
        }
        if (botRole.getUser().getId().equals(targetId)) {
            throw new ForbiddenOperationException("User cannot change their own role",
                    localizationLoader.localize(Error.SELF_CHANGE_ROLE, botRole));
        }
        LOGGER.info("Changing the role of user " + targetId + " in bot " + botRole.getBot().getId() + " to " + roleType + "...");

        botRoleFromDb.setRole(entityUtil.getRole(roleType));

        return botRoleFromDb;
    }

    @Transactional
    public BotRole banUserInBot(BotRole botRole, Long targetId, int hours) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");
        
        final BotRole botRoleFromDb = entityUtil.getBotRole(botRole, targetId);

        if (botRoleFromDb.getRole().getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .localize(Error.USER_ALREADY_BANNED, botRole));
        }
        if (botRoleFromDb.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .localize(Error.DIRECTOR_BAN, botRole));
        }
        if (botRoleFromDb.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator cannot be banned by a bot "
                    + "specific ban", localizationLoader.localize(
                    Error.CREATOR_BAN, botRole));
        }
        if (botRole.getUser().getId().equals(targetId)) {
            throw new ForbiddenOperationException("User cannot ban themselves", localizationLoader
                    .localize(Error.SELF_BAN, botRole));
        }
        LOGGER.info("Banning user " + targetId + " in bot " + botRole.getBot().getId() + "...");
        
        botRoleFromDb.setRole(entityUtil.getRole(RoleType.BANNED));

        if (hours > 0) {
            LOGGER.debug("Ban is temporary. Creating trigger...");
            timingService.createBanTrigger(botRoleFromDb, hours, false);
        }

        return botRoleFromDb;
    }

    @Transactional
    public BotRole liftBanInBot(BotRole targetBotRole) {
        Assert.notNull(targetBotRole, "targetBotRole cannot be null");

        return liftBanInBot0(null, targetBotRole);
    }

    @Transactional
    public BotRole liftBanInBot(BotRole botRole, Long targetId) {
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return liftBanInBot0(botRole, entityUtil.getActiveBotRole(botRole, targetId));
    }

    @Transactional
    public List<BotRole> banUserGenerally(BotRole callerBotRole, Long targetId, int hours) {
        Assert.notNull(callerBotRole, "callerBotRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        final UserEntity target = entityUtil.getUser(callerBotRole, targetId);
        final List<BotRole> targetBotRoles = botRoleRepository.findByUserId(targetId);

        if (targetBotRoles.isEmpty()) {
            throw new EntityNotFoundException("Unable to issue a general ban to user " + targetId
                    + " because they don't have a single bot role.", localizationLoader.localize(
                        Localizations.Error.GENERAL_BAN_NO_BOT_ROLES, callerBotRole));
        }
        if (target.isBanned()) {
            throw new ForbiddenOperationException("User " + targetId + " is already banned.", localizationLoader
                    .localize(Error.USER_ALREADY_BANNED, callerBotRole));
        }
        if (targetId.equals(directorId)) {
            throw new ForbiddenOperationException("Director cannot be banned.", localizationLoader
                    .localize(Error.DIRECTOR_BAN, callerBotRole));
        }
        LOGGER.debug("Director " + callerBotRole.getUser().getId() + " wants to give a general ban to user " + targetId + ".");

        if (hours > 0) {
            LOGGER.debug("General ban is temporary. Creating trigger...");
            timingService.createBanTrigger(targetBotRoles.getFirst(), hours, true);
        }

        target.setBanned(true);

        return botRoleRepository.findByUserId(targetId);
    }

    @Transactional
    public List<BotRole> liftGeneralBan(BotRole callerBotRole, Long targetId) {
        Assert.notNull(callerBotRole, "callerBotRole cannot be null");
        Assert.notNull(targetId, "targetId cannot be null");

        return liftGeneralBan0(callerBotRole, targetId);
    }

    @Transactional
    public List<BotRole> liftGeneralBan(BotRole targetBotRole) {
        Assert.notNull(targetBotRole, "targetBotRole cannot be null");

        return liftGeneralBan0(null, targetBotRole.getUser().getId());
    }

    /**
     * Creates or updates user entity if anything's changed. Returns the user.
     */
    @Transactional
    public UserEntity updateUser(UserEntity user) {
        LOGGER.trace("Checking if user " + user.getId() + "' data is up to date...");

        final UserEntity userFromDb;
        final Optional<UserEntity> potentialUser = userRepository.findById(user.getId());

        if (potentialUser.isEmpty()) {
            userFromDb = new UserEntity();
        } else {
            userFromDb = potentialUser.get();
        }

        userFromDb.setId(user.getId());
        userFromDb.setFirstName(user.getFirstName());
        userFromDb.setLastName(user.getLastName());
        userFromDb.setUsername(user.getUsername());
        
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
    public BotRole toggleReceiveHomework(BotRole botRole) {
        final BotRole botRoleFromDb = entityUtil.getBotRoleById(botRole, botRole.getId());
        
        botRoleFromDb.setReceivingHomework(!botRoleFromDb.isReceivingHomework());

        LOGGER.info("Receive homework for user " + botRoleFromDb.getUser().getId() + " in bot "
                + botRoleFromDb.getBot().getId() + " is now " + getStatus(botRoleFromDb.isReceivingHomework()) + ".");

        return botRoleRepository.save(botRoleFromDb);
    }

    @Transactional
    public UserEntity changeLanguage(BotRole botRole, String newCode) {
        final UserEntity userFromDb = entityUtil.getUser(botRole, botRole.getUser().getId());

        LOGGER.info("Changning language for user " + userFromDb.getId() + " to " + newCode + "...");

        userFromDb.setLanguageCode(newCode);
        userFromDb.setLanguageManuallySet(true);

        return userFromDb;
    }

    @Transactional
    public UserEntity resetLanguageToDefault(BotRole botRole) {
        final UserEntity userFromDb = entityUtil.getUser(botRole, botRole.getUser().getId());

        LOGGER.info("Resetting language code to default for user " + userFromDb.getId() + "...");

        userFromDb.setLanguageManuallySet(false);

        return userFromDb;
    }

    private String getStatus(boolean status) {
        return status ? "ENABLED" : "DISABLED";
    }

    private BotRole liftBanInBot0(BotRole callerBotRole, BotRole targetBotRole) {
        if (targetBotRole.getRole().getType() != RoleType.BANNED) {
            if (callerBotRole == null) {
                LOGGER.debug("User " + targetBotRole.getUser().getId() + " does not have a ban. The timed trigger request will be ignored.");
                return targetBotRole;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .localize(Error.USER_IS_NOT_BANNED, callerBotRole));
            }
        }
        LOGGER.info("User " + targetBotRole.getUser().getId() + " is banned. Removing ban...");

        targetBotRole.setRole(entityUtil.getRole(RoleType.USER));

        if (callerBotRole != null) {
            final int triggersRemoved = timingService.removeBanTriggerIfPresent(targetBotRole.getId());
            
            LOGGER.debug(triggersRemoved + " ban triggers have been removed for user " + targetBotRole.getUser().getId()
                    + " in bot " + targetBotRole.getBot().getId() + ".");
        }

        return targetBotRole;
    }

    private List<BotRole> liftGeneralBan0(BotRole callerBotRole, Long targetId) {
        final UserEntity target = entityUtil.getUser(callerBotRole, targetId);

        if (!target.isBanned()) {
            if (callerBotRole == null) {
                LOGGER.debug("User " + target.getId() + " does not have a ban. The timed trigger request will be ignored.");
                return List.of();
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .localize(Error.USER_IS_NOT_BANNED, callerBotRole));
            }
        }
        LOGGER.debug("User " + target.getId() + " has a general ban. Removing ban...");

        target.setBanned(false);

        if (callerBotRole != null) {        
            timingService.removeGeneralBanTriggerIfPresent(target.getId());
        }

        return botRoleRepository.findByUserId(targetId);
    }
}
