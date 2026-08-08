package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.localization.Localizations;
import com.unbidden.telegramcoursesbot.localization.Localizations.Error;
import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import com.unbidden.telegramcoursesbot.util.EntityUtil;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger LOGGER = LogManager.getLogger(UserService.class);

    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";

    private final UserRepository userRepository;

    private final BotRoleRepository botRoleRepository;

    private final LocalizationLoader localizationLoader;

    private final TimingService timingService;

    private final EntityUtil entityUtil;

    @Autowired
    @Lazy
    private ClientManager clientManager;

    @Value("${telegram.bot.authorization.director.id}")
    private Long directorId;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Transactional
    public UserEntity initializeUserForBot(User rawUser, Bot bot) {
        final UserEntity user = updateUser(rawUser);

        if (botRoleRepository.existsByBotIdAndUserId(bot.getId(), user.getId())) {
            return user;
        }
        
        LOGGER.debug("New user " + user.getId() + " is being registered in bot " + bot.getId() + "...");
        botRoleRepository.save(new BotRole(bot, user, entityUtil.getRole(RoleType.USER), false));
        LOGGER.debug("User " + user.getId() + " has been registered in bot " + bot.getId() + ".");
        return user;
    }

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

    public BotRole setRole(UserEntity user, UserEntity target, Bot bot, Role role) {
        final BotRole botRole = entityUtil.getBotRole(target, bot);

        if (role.getType().equals(RoleType.DIRECTOR) || role.getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Director and Creator roles are predefined",
                    localizationLoader.getLocalizationForUser(Error.PREDEFINED_CHANGE_ROLES,
                    user));
        }
        if (role.getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("Bans must be given through different means",
                    localizationLoader.getLocalizationForUser(Error.CANNOT_SET_BANNED_ROLE,
                    user));
        }
        if (role.equals(botRole.getRole())) {
            throw new ForbiddenOperationException("Role is the same",
                    localizationLoader.getLocalizationForUser(Error.SAME_ROLE, user));
        }
        if (botRole.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director's role is permanent",
                    localizationLoader.getLocalizationForUser(Error.DIRECTOR_CHANGE_ROLE, user));
        }
        if (botRole.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator's role is permanent",
                    localizationLoader.getLocalizationForUser(Error.CREATOR_CHANGE_ROLE, user));
        }
        if (user.equals(target)) {
            throw new ForbiddenOperationException("User cannot change their own role",
                    localizationLoader.getLocalizationForUser(Error.SELF_CHANGE_ROLE, user));
        }
        LOGGER.debug("Role change checks have passed. Applying...");
        botRole.setRole(role);
        botRoleRepository.save(botRole);
        LOGGER.debug("New bot role " + role.getType() + " persisted for user "
                + target.getId() + ".");

        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .getLocalizationForUser(Localizations.Service.ROLE_CHANGED, user,
                    new Localizations.Service.RoleChangedParams(user.getFullName(), role.getType(),
                    entityUtil.getLocalizedTitle(target, bot, user))));
        return botRole;
    }

    public BotRole banUserInBot(UserEntity user, UserEntity target, Bot bot, int hours) {
        final BotRole botRole = entityUtil.getBotRole(target, bot);

        if (botRole.getRole().getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .getLocalizationForUser(Error.USER_ALREADY_BANNED, user));
        }
        if (botRole.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .getLocalizationForUser(Error.DIRECTOR_BAN, user));
        }
        if (botRole.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator cannot be banned by a bot "
                    + "specific ban", localizationLoader.getLocalizationForUser(
                    Error.CREATOR_BAN, user));
        }
        if (user.equals(target)) {
            throw new ForbiddenOperationException("User cannot ban themselves", localizationLoader
                    .getLocalizationForUser(Error.SELF_BAN, user));
        }
        LOGGER.info("Banning user " + target.getId() + "...");
        botRole.setRole(entityUtil.getRole(RoleType.BANNED));
        final String title = entityUtil.getLocalizedTitle(target, bot, user);

        if (hours > 0) {
            LOGGER.debug("Ban is temporary. Creating trigger...");
            timingService.createTrigger(target, bot, hours, false);
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.TEMPORARY_BANNED, target,
                        new Localizations.Service.TemporaryBannedParams(user.getFullName(), hours, title)));
        } else {
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.BANNED, target,
                        new Localizations.Service.BannedParams(user.getFullName(), title)));
        }
        botRoleRepository.save(botRole);
        LOGGER.info("User " + target.getId() + " has been banned in bot " + bot.getId() + ".");
        return botRole;
    }

    public BotRole liftBanInBot(@Nullable UserEntity user, UserEntity target,
            Bot bot) {
        final BotRole botRole = entityUtil.getBotRole(target, bot);

        if (!botRole.getRole().getType().equals(RoleType.BANNED)) {
            if (user == null) {
                LOGGER.debug("User does not have a ban. Ignoring.");
                return botRole;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .getLocalizationForUser(Error.USER_IS_NOT_BANNED, user));
            }
        }
        LOGGER.debug("User " + target.getId() + " is banned. Removing ban...");
        botRole.setRole(entityUtil.getRole(RoleType.USER));
        if (user == null) {
            LOGGER.debug("Ban is being lifted automatically.");
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.BAN_LIFTED_AUTO, target));
        } else {
            LOGGER.debug("Ban lift is manual.");
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.BAN_LIFTED, target,
                        new Localizations.Service.BanLiftedParams(user.getFullName(),
                        entityUtil.getLocalizedTitle(target, bot, user))));

            final Optional<BanTrigger> potentialTrigger = timingService.findBanTrigger(user, bot);
            if (potentialTrigger.isPresent()) {
                LOGGER.debug("There is a trigger. Removing...");
                timingService.removeTrigger(potentialTrigger.get());
            }
        }
        botRoleRepository.save(botRole);
        LOGGER.info("Ban for user " + target.getId() + " in bot " + bot.getId() + " has been lifted.");
        return botRole;
    }

    public UserEntity banUserGenerally(UserEntity user, UserEntity target,
            int hours) {
        if (target.isBanned()) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .getLocalizationForUser(Error.USER_ALREADY_BANNED, user));
        }
        if (target.getId().equals(directorId)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .getLocalizationForUser(Error.DIRECTOR_BAN, target));
        }
        LOGGER.debug("User " + user.getId() + " wants to ban user " + target.getId() + ".");
        final String title = entityUtil.getLocalizedTitle(target, entityUtil.getStartBot(), user);
        final var temporaryBanParams = new Localizations.Service.TemporaryGeneralBanParams(user.getFullName(), hours, title);
        final var banParams = new Localizations.Service.GeneralBanParams(user.getFullName(), title);

        if (hours > 0) {
            LOGGER.debug("General ban is temporary. Creating trigger...");
            timingService.createTrigger(target, entityUtil.getStartBot(), hours, true);
            botRoleRepository.findByUserId(user.getId()).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader.getLocalizationForUser(
                    Localizations.Service.TEMPORARY_GENERAL_BAN, user, temporaryBanParams)));
        } else {
            botRoleRepository.findByUserId(user.getId()).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader.getLocalizationForUser(
                    Localizations.Service.GENERAL_BAN, user, banParams)));
        }
        target.setBanned(true);
        userRepository.save(target);
        LOGGER.info("User " + target.getId() + " has been completely banned.");
        return target;
    }

    public UserEntity liftGeneralBan(@Nullable UserEntity user, UserEntity target) {
        if (!target.isBanned()) {
            if (user == null) {
                LOGGER.debug("User does not have a ban. Ignoring.");
                return target;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .getLocalizationForUser(Error.USER_IS_NOT_BANNED, user));
            }
        }
        LOGGER.debug("User " + target.getId() + " has a general ban. Removing ban...");
        target.setBanned(false);
        if (user == null) {
            LOGGER.debug("Ban is being lifted automatically.");
            botRoleRepository.findByUserId(target.getId()).forEach(br -> clientManager
                    .getClient(br.getBot()).sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.GENERAL_BAN_LIFTED_AUTO, target)));
        } else {
            LOGGER.debug("Ban lift is manual.");
            botRoleRepository.findByUserId(user.getId()).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader
                    .getLocalizationForUser(Localizations.Service.GENERAL_BAN_LIFTED, target,
                        new Localizations.Service.GeneralBanLiftedParams(user.getFullName(),
                        entityUtil.getLocalizedTitle(target, entityUtil.getStartBot(), user)))));

            final Optional<BanTrigger> potentialTrigger = timingService.findBanTrigger(target,
                    entityUtil.getStartBot());
            if (potentialTrigger.isPresent()) {
                LOGGER.debug("There is a trigger. Removing...");
                timingService.removeTrigger(potentialTrigger.get());
            }
        }
        userRepository.save(target);
        LOGGER.info("General ban for user " + target.getId() + " has been lifted.");
        return target;
    }

    /**
     * Creates or updates user entity if anything changed. Returns the user.
     */
    public UserEntity updateUser(User user) {
        LOGGER.trace("Checking if user " + user.getId() + "' data is up to date...");
        final UserEntity userFromDb;
        final Optional<UserEntity> potentialUser = userRepository.findById(user.getId());
        boolean hasChanged = false;
        if (potentialUser.isEmpty()) {
            userFromDb = new UserEntity(user);
            hasChanged = true;
        } else {
            userFromDb = potentialUser.get();
        }
        if (!user.getFirstName().equals(userFromDb.getFirstName())) {
            userFromDb.setFirstName(user.getFirstName());
            hasChanged = true;
            LOGGER.trace("First name is " + user.getFirstName() + ". Setting...");
        }
        if (!userFromDb.isLanguageManuallySet()) {
            if (user.getLanguageCode() != null) {
                if (!user.getLanguageCode().equals(
                        userFromDb.getLanguageCode())) {
                    userFromDb.setLanguageCode(user.getLanguageCode());
                    hasChanged = true;
                    LOGGER.trace("Language code is " + user.getLanguageCode() + ". Setting...");
                }
            } else {
                final String theMostPreferedLanguage = languagePriorityStr
                        .split(LANGUAGE_PRIORITY_DIVIDER)[0].trim();
                userFromDb.setLanguageCode(theMostPreferedLanguage);
                hasChanged = true;
                LOGGER.trace("Language code is unavailable. Setting to "
                        + theMostPreferedLanguage + "...");
            }
        }
        if (user.getLastName() != null && !user.getLastName()
                .equals(userFromDb.getLastName())) {
            userFromDb.setLastName(user.getLastName());
            hasChanged = true;
            LOGGER.trace("Last name is " + user.getLastName() + ". Setting...");
        }
        if (user.getUserName() != null && !user.getUserName()
                .equals(userFromDb.getUsername())) {
            userFromDb.setUsername(user.getUserName());
            hasChanged = true;
            LOGGER.trace("Username is " + user.getUserName() + ". Setting...");
        }
        if (hasChanged) {
            LOGGER.trace("Stuff has changed for user " + user.getId() + ". Persisting...");
            userRepository.save(userFromDb);
            LOGGER.trace("Persist is successful.");
        } else {
            LOGGER.trace("User data is up to date with telegram servers.");
        }
        return userFromDb;
    }

    public BotRole toggleReceiveHomework(UserEntity user, Bot bot) {
        final BotRole botRole = entityUtil.getBotRole(user, bot);
        
        botRole.setReceivingHomework(!botRole.isReceivingHomework());
        return botRoleRepository.save(botRole);
    }

    public List<UserEntity> getHomeworkReceivingUsers(Bot bot) {
        return userRepository.findByReceivingHomeworkInBot(bot.getId());
    }

    public UserEntity changeLanguage(UserEntity user, String newCode) {
        LOGGER.info("Changning language for user " + user.getId() + " to " + newCode + "...");
        user.setLanguageCode(newCode);
        user.setLanguageManuallySet(true);
        userRepository.save(user);
        LOGGER.info("Language code for user " + user.getId() + " has been changed to "
                + newCode + ".");
        return user;
    }

    public UserEntity resetLanguageToDefault(UserEntity user) {
        LOGGER.info("Resetting language code to default for user " + user.getId() + "...");
        user.setLanguageManuallySet(false);
        userRepository.save(user);
        LOGGER.info("Language code for user " + user.getId() + " is now automatically set.");
        return user;
    }
}
