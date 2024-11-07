package com.unbidden.telegramcoursesbot.service.localization;

import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.exception.LocalizationLoadingException;
import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.repository.LocalizationRepository;
import com.unbidden.telegramcoursesbot.util.Tag;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class LocalizationLoaderImpl implements LocalizationLoader {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationLoaderImpl.class);

    @Value("${telegram.bot.message.text.format}")
    private String fileFormat;

    @Value("${telegram.bot.message.text.path}")
    private String pathStr;

    private Path localizationFolderPath;

    private List<String> languagePriority;

    @Autowired
    private TextUtil textUtil;

    @Autowired
    private LocalizationDao dao;

    @Autowired
    private LocalizationRepository localizationRepository;

    @PostConstruct
    private void init() {
        localizationFolderPath = Path.of(System.getProperty("user.dir")).resolve(pathStr);
        LOGGER.info("Initialized localization files directory in "
                + localizationFolderPath + ".");

        languagePriority = new ArrayList<>();
        String[] languagePriorityArray = textUtil.getLanguagePriority();
        if (languagePriorityArray.length == 0) {
            throw new LocalizationLoadingException("At least one language code should "
                    + "be present in the priority list");
        }
        for (String code : languagePriorityArray) {
            languagePriority.add(code.trim());
        }

        cacheLocalizationFiles();
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull User user) {
        final Localization localization = loadLocalization(name, user.getLanguageCode());
        
        if (!localization.isInjectionRequired()) {
            return localization;
        }
        final String withInjectedUserData = textUtil.injectUserData(localization.getData(), user);
        LOGGER.trace("User data injected. Setting up entities...");
        return setUpLocalization(localization, withInjectedUserData);
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull UserEntity user) {
        final Localization localization = loadLocalization(name, user.getLanguageCode());
        
        if (!localization.isInjectionRequired()) {
            return localization;
        }
        final String withInjectedUserData = textUtil.injectUserData(localization.getData(), user);
        LOGGER.trace("User data injected. Setting up entities...");
        return setUpLocalization(localization, withInjectedUserData);
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull User user,
            @NonNull Map<String, Object> parameterMap) {
        final Localization localization = loadLocalization(name, user.getLanguageCode());

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        final String withInjectedParams = textUtil.injectParams(textUtil.injectUserData(
                localization.getData(), user), parameterMap);
        LOGGER.trace("User data and custom parameters injected. Setting up entities...");
        return setUpLocalization(localization, withInjectedParams);
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull UserEntity user,
            @NonNull Map<String, Object> parameterMap) {
        final Localization localization = loadLocalization(name, user.getLanguageCode());

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        final String withInjectedParams = textUtil.injectParams(textUtil.injectUserData(
                localization.getData(), user), parameterMap);
        LOGGER.trace("User data and custom parameters injected. Setting up entities...");
        return setUpLocalization(localization, withInjectedParams);
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull User user,
            @NonNull String paramKey, @NonNull Object param) {
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(paramKey, param);
        return getLocalizationForUser(name, user, parameterMap);
    }

    @Override
    @NonNull
    public Localization getLocalizationForUser(@NonNull String name, @NonNull UserEntity user,
            @NonNull String paramKey, @NonNull Object param) {
        final Map<String, Object> parameterMap = new HashMap<>();

        parameterMap.put(paramKey, param);
        return getLocalizationForUser(name, user, parameterMap);
    }

    @Override
    public void reloadResourses() {
        localizationRepository.clear();
        cacheLocalizationFiles();
    }

    @Override
    @NonNull
    public Localization loadLocalization(@NonNull String name, @NonNull String languageCode) {
        LOGGER.trace("Loading cached localization " + name + "...");
        Localization localization = findAvailableLocalization(name, languageCode);

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        LOGGER.trace("Localization requires parameter injection. Creating copy...");
        try {
            localization = (Localization) localization.clone();
        } catch (CloneNotSupportedException e) {
            throw new LocalizationLoadingException("Cloning of localization is not supported.");
        }
        return localization;
    }

    @Override
    @NonNull
    public List<String> getAvailableLanguageCodes() {
        return dao.list(localizationFolderPath).stream()
                .filter(p -> p.toFile().isDirectory())
                .map(p -> p.getFileName().toString())
                .toList();
    }

    private void cacheLocalizationFiles() {
        LOGGER.trace("Localization files caching is commencing...");
        final List<Path> locDirs = dao.list(localizationFolderPath).stream()
                .filter(p -> p.toFile().isDirectory())
                .toList();

        
        for (Path locDir : locDirs) {
            final List<Path> locFiles = dao.list(locDir).stream()
                    .filter(p -> p.toFile().isFile())
                    .toList();

            LOGGER.trace("There are " + locFiles.size() + " localization files in "
                    + locDir.getFileName() + ": " + locFiles.stream()
                    .map(p -> p.getFileName().toString()).toList().toString() + ".");

            LOGGER.trace("Checking that all specified priority language codes "
                    + "have a directory...");
            final List<String> locDirNames = locDirs.stream()
                    .map(ld -> ld.getFileName().toString()).toList();
            final List<String> priorityCodesWithNoDir = languagePriority.stream()
                    .filter(pc -> !locDirNames.contains(pc)).toList();
            if (priorityCodesWithNoDir.size() != 0) {
                throw new LocalizationLoadingException("Some specified priority language codes "
                        + "do not have any directories. Those are: "
                        + priorityCodesWithNoDir.toString());
            }
            LOGGER.trace("Everything is a go.");
            for (Path locFile : locFiles) {
                final String keyPattern = FilenameUtils.getBaseName(locFile.toString()) + "_%s_"
                        + locDir.getFileName();
                LOGGER.trace("Working on file " + locFile.getFileName().toString()
                        + ". Key pattern is going to be: " + keyPattern.formatted("<tag>") + ".");

                try {
                    Map<Tag, String> tagedContent = textUtil.getMappedTagContent(
                            dao.getText(locFile));
                    for (Entry<Tag, String> entry : tagedContent.entrySet()) {
                        final String key = keyPattern.formatted(entry.getKey().getName());
                        final String content = textUtil.removeEndLineOverrides(entry.getValue());
                        final Localization newLocalization;

                        if (entry.getKey().isInjectionRequired()) {
                            LOGGER.trace("Localization " + key + " has custom parameters "
                                    + "that will need to be injected later.");
                            newLocalization = new Localization(key, content, true);
                        } else {
                            LOGGER.trace("Localization " + key + " does not have any custom "
                                    + "parameters. Parsing markers now...");
                            final List<MessageEntity> entities =
                                    textUtil.getEntities(content);
                            newLocalization = new Localization(key,
                                    textUtil.removeMarkers(content), false);
                            newLocalization.setEntities(entities);
                        }
                        LOGGER.trace("Saving localization data...");
                        localizationRepository.save(newLocalization);
                    }
                    LOGGER.trace("Localization data from file " + locFile + " has been cached.");
                } catch (TaggedStringInterpretationException e) {
                    throw new LocalizationLoadingException("Unable to parse file " + locFile, e);
                }
            }
        }
        LOGGER.trace("Localization files cached successfuly.");
    }

    private Localization setUpLocalization(Localization localization, String injectedData) {
        localization.setEntities(textUtil.getEntities(injectedData));
        localization.setData(textUtil.removeMarkers(injectedData));
        LOGGER.trace("Entities set up.");
        return localization;
    }

    private Localization findAvailableLocalization(String name, String preferableLanguageCode) {
        Optional<Localization> potentialLoc = localizationRepository
                .find(name + "_" + preferableLanguageCode);
        
        if (potentialLoc.isPresent()) {
            LOGGER.trace("Localization " + name + " for prefered code " + preferableLanguageCode
                    + " is available.");
            return potentialLoc.get();
        }
        LOGGER.trace("Localization " + name + " for prefered code " + preferableLanguageCode
                + " is not available. Looking over the language code priority list...");
        for (String code : languagePriority) {
            if (!code.equals(preferableLanguageCode)) {
                potentialLoc = localizationRepository.find(name + "_" + code);
                if (potentialLoc.isPresent()) {
                    LOGGER.trace("Localization " + name + " found for code " + code + ".");
                    return potentialLoc.get();
                }
            }
        }
        LOGGER.warn("No localization with name " + name
                + " was found. The name will be sent instead.");
        return new Localization(name);
    }
}
