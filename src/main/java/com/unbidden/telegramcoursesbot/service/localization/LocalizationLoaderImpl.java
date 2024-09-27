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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    @Value("${telegram.bot.message.language.default}")
    private String defaultLanguageCode;

    @Value("${telegram.bot.message.text.path}")
    private String pathStr;

    private Path localizationFolderPath;

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
        LOGGER.info("User data injected. Setting up entities...");
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
        LOGGER.info("User data injected. Setting up entities...");
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
        LOGGER.info("User data and custom parameters injected. Setting up entities...");
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
        LOGGER.info("User data and custom parameters injected. Setting up entities...");
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
    public void reloadResourses() {
        localizationRepository.clear();
        cacheLocalizationFiles();
    }

    @Override
    @NonNull
    public Localization loadLocalization(@NonNull String name, @NonNull String languageCode) {
        LOGGER.info("Loading cached localization " + name + "...");
        Localization localization = localizationRepository.find(name + "_"
                + languageCode).orElse(localizationRepository.find(name + "_"
                + defaultLanguageCode).orElse(new Localization(name)));

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        LOGGER.info("Localization requires parameter injection. Creating copy...");
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
        LOGGER.info("Localization files caching is commencing...");
        final List<Path> locDirs = dao.list(localizationFolderPath).stream()
                .filter(p -> p.toFile().isDirectory())
                .toList();

        LOGGER.info("Checking for default localization directory...");
        if (locDirs.stream()
                .filter(p -> p.getFileName().toString().equals(defaultLanguageCode))
                .toList()
                .isEmpty()) {
            throw new LocalizationLoadingException("No localization directory for "
                    + defaultLanguageCode + " was found by path " + localizationFolderPath);
        }
        LOGGER.info("There are " + locDirs.size() + " localization directories present: "
                + locDirs.stream().map(p -> p.getFileName().toString()).toList().toString()
                + " where default one is " + defaultLanguageCode);

        for (Path locDir : locDirs) {
            final List<Path> locFiles = dao.list(locDir).stream()
                    .filter(p -> p.toFile().isFile())
                    .toList();

            LOGGER.info("There are " + locFiles.size() + " localization files in "
                    + locDir.getFileName() + ": " + locFiles.stream()
                    .map(p -> p.getFileName().toString()).toList().toString() + ".");
            
            for (Path locFile : locFiles) {
                final String keyPattern = FilenameUtils.getBaseName(locFile.toString()) + "_%s_"
                        + locDir.getFileName();
                LOGGER.info("Working on file " + locFile.getFileName().toString()
                        + ". Key pattern is going to be: " + keyPattern.formatted("<tag>") + ".");

                try {
                    Map<Tag, String> tagedContent = textUtil.getMappedTagContent(
                            dao.getText(locFile));
                    for (Entry<Tag, String> entry : tagedContent.entrySet()) {
                        final String key = keyPattern.formatted(entry.getKey().getName());
                        final String content = textUtil.removeEndLineOverrides(entry.getValue());
                        final Localization newLocalization;

                        if (entry.getKey().isInjectionRequired()) {
                            LOGGER.info("Localization " + key + " has custom parameters "
                                    + "that will need to be injected later.");
                            newLocalization = new Localization(key, content, true);
                        } else {
                            LOGGER.info("Localization " + key + " does not have any custom "
                                    + "parameters. Parsing markers now...");
                            final List<MessageEntity> entities =
                                    textUtil.getEntities(content);
                            newLocalization = new Localization(key,
                                    textUtil.removeMarkers(content), false);
                            newLocalization.setEntities(entities);
                        }
                        LOGGER.info("Saving localization data...");
                        localizationRepository.save(newLocalization);
                    }
                    LOGGER.info("Localization data from file " + locFile + " has been cached.");
                } catch (TaggedStringInterpretationException e) {
                    throw new LocalizationLoadingException("Unable to parse file " + locFile, e);
                }
            }
        }
        LOGGER.info("Localization files cached successfuly.");
    }

    private Localization setUpLocalization(Localization localization, String injectedData) {
        localization.setEntities(textUtil.getEntities(injectedData));
        localization.setData(textUtil.removeMarkers(injectedData));
        LOGGER.info("Entities set up.");
        return localization;
    }
}
