package com.unbidden.telegramcoursesbot.service.user;

import com.unbidden.telegramcoursesbot.bot.BotService;
import com.unbidden.telegramcoursesbot.bot.ClientManager;
import com.unbidden.telegramcoursesbot.exception.EntityNotFoundException;
import com.unbidden.telegramcoursesbot.exception.ForbiddenOperationException;
import com.unbidden.telegramcoursesbot.model.Authority;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import com.unbidden.telegramcoursesbot.model.BanTrigger;
import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.model.Role;
import com.unbidden.telegramcoursesbot.model.RoleType;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.AuthorityRepository;
import com.unbidden.telegramcoursesbot.repository.BotRoleRepository;
import com.unbidden.telegramcoursesbot.repository.RoleRepository;
import com.unbidden.telegramcoursesbot.repository.UserRepository;
import com.unbidden.telegramcoursesbot.service.localization.LocalizationLoader;
import com.unbidden.telegramcoursesbot.service.timing.TimingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LogManager.getLogger(UserServiceImpl.class);
    
    private static final String PARAM_TITLE = "${title}";
    private static final String PARAM_HOURS_UNTIL_LIFT = "${hoursUntilLift}";
    private static final String PARAM_NEW_ROLE_TYPE = "${newRoleType}";
    private static final String PARAM_WHO_CHANGED = "${whoChanged}";
    private static final String PARAM_WHO_BANNED = "${whoBanned}";
    private static final String PARAM_WHO_LIFTED = "${whoLifted}";
    
    private static final String SERVICE_BANNED = "service_banned";
    private static final String SERVICE_TEMPORARY_BANNED = "service_temporary_banned";
    private static final String SERVICE_ROLE_CHANGED = "service_role_changed";
    private static final String SERVICE_BAN_LIFTED = "service_ban_lifted";
    private static final String SERVICE_ROLE_TITLE = "service_role_%s_title";
    private static final String SERVICE_BAN_LIFTED_AUTO = "service_ban_lifted_auto";
    private static final String SERVICE_GENERAL_BAN_LIFTED_AUTO =
            "service_general_ban_lifted_auto";
    private static final String SERVICE_GENERAL_BAN_LIFTED = "service_general_ban_lifted";
    private static final String SERVICE_TEMPORARY_GENERAL_BAN = "service_temporary_general_ban";
    private static final String SERVICE_GENERAL_BAN = "service_general_ban";
    
    private static final String ERROR_USER_IS_NOT_BANNED = "error_user_is_not_banned";
    private static final String ERROR_USER_ALREADY_BANNED = "error_user_already_banned";
    private static final String ERROR_CANNOT_SET_BANNED_ROLE = "error_cannot_set_banned_role";
    private static final String ERROR_USER_NOT_FOUND = "error_user_not_found";
    private static final String ERROR_BOT_ROLE_NOT_FOUND = "error_bot_role_not_found";
    private static final String ERROR_CREATOR_BAN = "error_creator_ban";
    private static final String ERROR_SELF_BAN = "error_self_ban";
    private static final String ERROR_SAME_ROLE = "error_same_role";
    private static final String ERROR_PREDEFINED_CHANGE_ROLES = "error_predefined_change_roles";
    private static final String ERROR_SELF_CHANGE_ROLE = "error_self_change_role";
    private static final String ERROR_CREATOR_CHANGE_ROLE = "error_creator_change_role";
    private static final String ERROR_DIRECTOR_CHANGE_ROLE = "error_director_change_role";
    private static final String ERROR_DIRECTOR_BAN = "error_director_ban";
    
    private static final String LANGUAGE_PRIORITY_DIVIDER = ",";

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final AuthorityRepository authorityRepository;

    private final BotRoleRepository botRoleRepository;

    private final LocalizationLoader localizationLoader;

    private final TimingService timingService;

    private final BotService botService;

    @Autowired
    @Lazy
    private ClientManager clientManager;

    @Value("${telegram.bot.authorization.director.id}")
    private Long directorId;

    @Value("${telegram.bot.message.language.priority}")
    private String languagePriorityStr;

    @Override
    @NonNull
    public UserEntity initializeUserForBot(@NonNull User rawUser, @NonNull Bot bot) {
        final UserEntity user = updateUser(rawUser);

        if (botRoleRepository.findByBotAndUser(bot, user).isPresent()) {
            return user;
        }
        
        LOGGER.debug("New user " + user.getId() + " is being registered in bot "
                + bot.getName() + "...");
        botRoleRepository.save(new BotRole(bot, user, getRole(RoleType.USER), false));
        LOGGER.debug("User " + user.getId() + " has been registered in bot "
                + bot.getName() + ".");
        return user;
    }

    @Override
    @NonNull
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

    @Override
    @NonNull
    public Role getRole(@NonNull RoleType type) {
        return roleRepository.findByType(type).orElseThrow(() ->
                new EntityNotFoundException("Role with type " + type
                + " does not exist", null));
    }

    @Override
    @NonNull
    public BotRole getBotRole(@NonNull UserEntity user, @NonNull Bot bot) {
        return botRoleRepository.findByBotAndUser(bot, user).orElseThrow(() ->
                new EntityNotFoundException("Bot role for user " + user.getId()
                + " and bot " + bot.getId() + " does not exist", localizationLoader
                .getLocalizationForUser(ERROR_BOT_ROLE_NOT_FOUND, user)));
    }

    @Override
    @NonNull
    public BotRole setRole(@NonNull UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot, @NonNull Role role) {
        final BotRole botRole = getBotRole(target, bot);

        if (role.getType().equals(RoleType.DIRECTOR) || role.getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Director and Creator roles are predefined",
                    localizationLoader.getLocalizationForUser(ERROR_PREDEFINED_CHANGE_ROLES,
                    user));
        }
        if (role.getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("Bans must be given through different means",
                    localizationLoader.getLocalizationForUser(ERROR_CANNOT_SET_BANNED_ROLE,
                    user));
        }
        if (role.equals(botRole.getRole())) {
            throw new ForbiddenOperationException("Role is the same",
                    localizationLoader.getLocalizationForUser(ERROR_SAME_ROLE, user));
        }
        if (botRole.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director's role is permanent",
                    localizationLoader.getLocalizationForUser(ERROR_DIRECTOR_CHANGE_ROLE, user));
        }
        if (botRole.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator's role is permanent",
                    localizationLoader.getLocalizationForUser(ERROR_CREATOR_CHANGE_ROLE, user));
        }
        if (user.equals(target)) {
            throw new ForbiddenOperationException("User cannot change their own role",
                    localizationLoader.getLocalizationForUser(ERROR_SELF_CHANGE_ROLE, user));
        }
        LOGGER.debug("Role change checks have passed. Applying...");
        botRole.setRole(role);
        botRoleRepository.save(botRole);
        LOGGER.debug("New bot role " + role.getType() + " persisted for user "
                + target.getId() + ".");

        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_WHO_CHANGED, user.getFullName());
        parameterMap.put(PARAM_NEW_ROLE_TYPE, role.getType());
        parameterMap.put(PARAM_TITLE, getLocalizedTitle(user, bot));
        clientManager.getClient(bot).sendMessage(target, localizationLoader
                .getLocalizationForUser(SERVICE_ROLE_CHANGED, user, parameterMap));
        return botRole;
    }

    @Override
    @NonNull
    public BotRole banUserInBot(@NonNull UserEntity user,
            @NonNull UserEntity target, @NonNull Bot bot, int hours) {
        final BotRole botRole = getBotRole(target, bot);

        if (botRole.getRole().getType().equals(RoleType.BANNED)) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .getLocalizationForUser(ERROR_USER_ALREADY_BANNED, user));
        }
        if (botRole.getRole().getType().equals(RoleType.DIRECTOR)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .getLocalizationForUser(ERROR_DIRECTOR_BAN, user));
        }
        if (botRole.getRole().getType().equals(RoleType.CREATOR)) {
            throw new ForbiddenOperationException("Creator cannot be banned by a bot "
                    + "specific ban", localizationLoader.getLocalizationForUser(
                    ERROR_CREATOR_BAN, user));
        }
        if (user.equals(target)) {
            throw new ForbiddenOperationException("User cannot ban themselfs", localizationLoader
                    .getLocalizationForUser(ERROR_SELF_BAN, user));
        }
        LOGGER.info("Banning user " + target.getId() + "...");
        botRole.setRole(getRole(RoleType.BANNED));

        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_WHO_BANNED, user.getFullName());
        parameterMap.put(PARAM_TITLE, getLocalizedTitle(user, bot));
        parameterMap.put(PARAM_HOURS_UNTIL_LIFT, hours);

        if (hours > 0) {
            LOGGER.debug("Ban is temporary. Creating trigger...");
            timingService.createTrigger(target, bot, hours, false);
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_TEMPORARY_BANNED, target, parameterMap));
        } else {
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_BANNED, target, parameterMap));
        }
        botRoleRepository.save(botRole);
        LOGGER.info("User " + target.getId() + " has been banned in bot " + bot.getName() + ".");
        return botRole;
    }

    @Override
    @NonNull
    public BotRole liftBanInBot(@Nullable UserEntity user, @NonNull UserEntity target,
            @NonNull Bot bot) {
        final BotRole botRole = getBotRole(target, bot);
        if (!botRole.getRole().getType().equals(RoleType.BANNED)) {
            if (user == null) {
                LOGGER.debug("User does not have a ban. Ignoring.");
                return botRole;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .getLocalizationForUser(ERROR_USER_IS_NOT_BANNED, user));
            }
        }
        LOGGER.debug("User " + target.getId() + " is banned. Removing ban...");
        botRole.setRole(getRole(RoleType.USER));
        if (user == null) {
            LOGGER.debug("Ban is being lifted automatically.");
            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_BAN_LIFTED_AUTO, target));
        } else {
            LOGGER.debug("Ban lift is manual.");
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_WHO_LIFTED, user.getFullName());
            parameterMap.put(PARAM_TITLE, getLocalizedTitle(user, bot));

            clientManager.getClient(bot).sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_BAN_LIFTED, target, parameterMap));

            final Optional<BanTrigger> potentialTrigger = timingService.findBanTrigger(user, bot);
            if (potentialTrigger.isPresent()) {
                LOGGER.debug("There is a trigger. Removing...");
                timingService.removeTrigger(potentialTrigger.get());
            }
        }
        botRoleRepository.save(botRole);
        LOGGER.info("Ban for user " + target.getId() + " in bot " + bot.getName()
                + " has been lifted.");
        return botRole;
    }

    @Override
    @NonNull
    public UserEntity banUserGenerally(@NonNull UserEntity user, @NonNull UserEntity target,
            int hours) {
        if (target.isBanned()) {
            throw new ForbiddenOperationException("User is already banned", localizationLoader
                    .getLocalizationForUser(ERROR_USER_ALREADY_BANNED, user));
        }
        if (target.getId().equals(directorId)) {
            throw new ForbiddenOperationException("Director cannot be banned", localizationLoader
                    .getLocalizationForUser(ERROR_DIRECTOR_BAN, target));
        }
        LOGGER.debug("User " + user.getId() + " wants to ban user " + target.getId() + ".");
        final Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(PARAM_WHO_BANNED, user.getFullName());
        parameterMap.put(PARAM_TITLE, getLocalizedTitle(user, botService.getInitialBot()));
        parameterMap.put(PARAM_HOURS_UNTIL_LIFT, hours);

        if (hours > 0) {
            LOGGER.debug("General ban is temporary. Creating trigger...");
            timingService.createTrigger(target, botService.getInitialBot(), hours, true);
            botRoleRepository.findByUser(user).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader.getLocalizationForUser(
                    SERVICE_TEMPORARY_GENERAL_BAN, user, parameterMap)));
        } else {
            botRoleRepository.findByUser(user).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader.getLocalizationForUser(
                    SERVICE_GENERAL_BAN, user, parameterMap)));
        }
        target.setBanned(true);
        userRepository.save(target);
        LOGGER.info("User " + target.getId() + " has been completely banned.");
        return target;
    }

    @Override
    @NonNull
    public UserEntity liftGeneralBan(@Nullable UserEntity user, @NonNull UserEntity target) {
        if (!target.isBanned()) {
            if (user == null) {
                LOGGER.debug("User does not have a ban. Ignoring.");
                return target;
            } else {
                throw new ForbiddenOperationException("User is not banned", localizationLoader
                        .getLocalizationForUser(ERROR_USER_IS_NOT_BANNED, user));
            }
        }
        LOGGER.debug("User " + target.getId() + " has a general ban. Removing ban...");
        target.setBanned(false);
        if (user == null) {
            LOGGER.debug("Ban is being lifted automatically.");
            botRoleRepository.findByUser(target).forEach(br -> clientManager
                    .getClient(br.getBot()).sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_GENERAL_BAN_LIFTED_AUTO, target)));
        } else {
            LOGGER.debug("Ban lift is manual.");
            final Map<String, Object> parameterMap = new HashMap<>();
            parameterMap.put(PARAM_WHO_BANNED, user.getFullName());
            parameterMap.put(PARAM_TITLE, getLocalizedTitle(user, botService.getInitialBot()));
        
            botRoleRepository.findByUser(user).forEach(br -> clientManager.getClient(br.getBot())
                    .sendMessage(target, localizationLoader
                    .getLocalizationForUser(SERVICE_GENERAL_BAN_LIFTED, target, parameterMap)));

            final Optional<BanTrigger> potentialTrigger = timingService.findBanTrigger(target,
                    botService.getInitialBot());
            if (potentialTrigger.isPresent()) {
                LOGGER.debug("There is a trigger. Removing...");
                timingService.removeTrigger(potentialTrigger.get());
            }
        }
        userRepository.save(target);
        LOGGER.info("General ban for user " + target.getId() + " has been lifted.");
        return target;
    }

    @Override
    @NonNull
    public String getLocalizedTitle(@NonNull UserEntity user, @NonNull Bot bot) {
        final String roleNameLower = getBotRole(user, bot).getRole()
                .getType().toString().toLowerCase();
        return localizationLoader.getLocalizationForUser(SERVICE_ROLE_TITLE
                .formatted(roleNameLower), user).getData();
    }

    @Override
    @NonNull
    public UserEntity getUser(@NonNull Long id, @NonNull UserEntity user) {
        return userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User "
                + id + " is not registred in the database", localizationLoader
                .getLocalizationForUser(ERROR_USER_NOT_FOUND, user)));
    }

    /**
     * Creates or updates user entity if anything changed. Returns the user.
     */
    @Override
    @NonNull
    public UserEntity updateUser(@NonNull User user) {
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
        if (user.getLanguageCode() != null) {
            if (!user.getLanguageCode().equals(userFromDb.getLanguageCode())) {
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

    @Override
    @NonNull
    public BotRole toggleReceiveHomework(@NonNull UserEntity user, @NonNull Bot bot) {
        final BotRole botRole = getBotRole(user, bot);
        
        botRole.setReceivingHomework(!botRole.isReceivingHomework());
        return botRoleRepository.save(botRole);
    }

    @Override
    @NonNull
    public UserEntity getDiretor() {
        return userRepository.findById(directorId).get();
    }

    @Override
    @NonNull
    public UserEntity getCreator(@NonNull Bot bot) {
        final Optional<BotRole> potentialCreator = botRoleRepository.findCreator(bot.getId());
        if (potentialCreator.isEmpty()) {
            LOGGER.warn("Bot " + bot.getId() + " does not have any Creator. "
                    + "Returning Director instead...");
            return getDiretor();
        }
        return potentialCreator.get().getUser();
    }

    @Override
    @NonNull
    public List<UserEntity> getSupport(@NonNull Bot bot) {
        return botRoleRepository.findByBotAndRoleType(bot, RoleType.SUPPORT).stream()
                .map(br -> br.getUser()).toList();
    }

    @Override
    @NonNull
    public List<UserEntity> getMentors(@NonNull Bot bot) {
        return botRoleRepository.findByBotAndRoleType(bot, RoleType.MENTOR).stream()
                .map(br -> br.getUser()).toList();
    }

    @Override
    @NonNull
    public List<BotRole> getHomeworkBotRoles(@NonNull Bot bot) {
        return botRoleRepository.findByHomeworkRecievingInBot(bot.getId());
    }

    @Override
    @NonNull
    public List<Authority> parseAuthorities(@NonNull AuthorityType[] types) {
        final List<Authority> authorities = new ArrayList<>();
        for (AuthorityType type : types) {
            authorities.add(authorityRepository.findByType(type).orElseThrow(() ->
                    new EntityNotFoundException("Authority " + type + " does not exist", null)));
        }
        return authorities;
    }
}
