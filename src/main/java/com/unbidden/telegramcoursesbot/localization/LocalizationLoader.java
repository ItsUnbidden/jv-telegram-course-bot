package com.unbidden.telegramcoursesbot.localization;

import com.unbidden.telegramcoursesbot.dao.LocalizationDao;
import com.unbidden.telegramcoursesbot.exception.LocalizationLoadingException;
import com.unbidden.telegramcoursesbot.exception.TaggedStringInterpretationException;
import com.unbidden.telegramcoursesbot.localization.Localizations.LocalizationKey;
import com.unbidden.telegramcoursesbot.model.BotRole;
import com.unbidden.telegramcoursesbot.repository.LocalizationRepository;
import com.unbidden.telegramcoursesbot.util.TextUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

@Service
@RequiredArgsConstructor
public class LocalizationLoader {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationLoader.class);

    private final LocalizationDao dao;
    
    private final LocalizationRepository localizationRepository;

    private final TextUtil textUtil;

    private List<String> languagePriority;

    @PostConstruct
    private void init() {
        languagePriority = textUtil.getLanguagePriority();

        cacheLocalizationFiles();
    }

    public Localization localize(LocalizationKey key, BotRole botRole) {
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");
        
        final Localization localization = loadLocalization(key, botRole.getUser().getLanguageCode(),
                botRole.getBot().languagesToList());
        
        if (localization.isInjectionRequired()) {
            LOGGER.error("Localization \"" + localization.getName() + "\" is marked for parameter injection, "
                    + "but no parameters were supplied since the wrong method overload was called.");
        }
        
        return localization;
    }

    public Localization localize(LocalizationKey key, BotRole botRole, Object paramRecord) {
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notNull(paramRecord, "paramRecord cannot be null");
        
        final Localization localization = loadLocalization(key, botRole.getUser().getLanguageCode(),
                botRole.getBot().languagesToList());

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        final String withInjectedParams = textUtil.injectParams(localization.getData(),
                mapValuesToParamNames(localization, paramRecord));

        LOGGER.trace("Parameters injected. Setting up entities...");
        return setUpLocalization(localization, withInjectedParams);
    }

    public Localization localizeGeneric(LocalizationKey key, BotRole botRole, Object... args) {
        Assert.notNull(key, "key cannot be null");
        Assert.notNull(botRole, "botRole cannot be null");
        Assert.notEmpty(args, "args cannot be empty or null");
        
        final Localization localization = loadLocalization(key, botRole.getUser().getLanguageCode(),
                botRole.getBot().languagesToList(), args);
        
        if (localization.isInjectionRequired()) {
            LOGGER.error("Localization \"" + localization.getName() + "\" is marked for parameter injection, "
                    + "but no parameters were supplied since the wrong method overload was called.");
        }
        
        return localization;
    }

    public void reloadResourses() {
        localizationRepository.clear();
        cacheLocalizationFiles();
    }

    public Localization loadLocalization(LocalizationKey key, String languageCode, List<String> customLangPriority) {
        Assert.notNull(key, "name cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notNull(customLangPriority, "customLangPriority cannot be null");

        return loadLocalization(key, languageCode, customLangPriority, null);
    }

    public Localization loadGenericLocalization(LocalizationKey key, String languageCode,
            List<String> customLangPriority, Object... args) {
        Assert.notNull(key, "name cannot be null");
        Assert.notNull(languageCode, "languageCode cannot be null");
        Assert.notNull(customLangPriority, "customLangPriority cannot be null");

        return loadLocalization(key, languageCode, customLangPriority, args);
    }

    public List<String> getAvailableLanguageCodes() {
        return dao.listLocalizationDirs().stream()
                .filter(p -> p.toFile().isDirectory())
                .map(p -> p.getFileName().toString())
                .toList();
    }

    /**
     * Loads the original language name.
     * @param code
     * @return
     */
    public String getLanguageName(String code) {
        return loadGenericLocalization(Localizations.Service.LANGUAGE_CODE, code, List.of(), code).getData();
    }

    /**
     * Loads the name of the language localized for a specific user.
     * @param localizedFor
     * @param code
     * @return
     */
    public String getLanguageName(BotRole botRole, String code) {
        return localizeGeneric(Localizations.Service.LANGUAGE_CODE, botRole, code).getData();
    }

    private Localization loadLocalization(LocalizationKey key, String languageCode, List<String> customLangPriority, Object[] args) {
        LOGGER.trace("Loading cached localization " + key + "...");
        Localization localization = findAvailableLocalization((args == null ? key.getLocName() : key.getLocName().formatted(args)),
                languageCode, customLangPriority);

        if (!localization.isInjectionRequired()) {
            return localization;
        }
        LOGGER.trace("Localization requires parameter injection. Creating copy...");
        try {
            localization = (Localization) localization.clone();
        } catch (CloneNotSupportedException e) {
            throw new LocalizationLoadingException("Cloning of localization is not supported.",
                    null);
        }
        return localization;
    }

    private Map<String, Object> mapValuesToParamNames(Localization loc, Object record) {
        final Class<?> clazz = record.getClass();

        if (!clazz.isRecord()) throw new IllegalArgumentException("Localization parameters must be presented as records.");

        final Map<String, Object> result = new HashMap<>();

        for (final RecordComponent component : clazz.getRecordComponents()) {
            try {
                final Object value = component.getAccessor().invoke(record);
                final String name = "${" + component.getName() + "}";

                if (value == null) {
                    throw new IllegalArgumentException("Localization parameters must never be null. Record: "
                            + clazz.getSimpleName() + ", field name: " + name + ".");
                }

                if (!loc.getParams().contains(name)) {
                    LOGGER.warn("Parameter " + name + " is not present in the text of localization \"" + loc.getName() + "\".");
                }

                result.put(name, value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to access argument " + component.getName()
                        + " on record of class " + clazz.getName() + ". Object: " + record);
            }
        }
        final List<String> missingParams = new ArrayList<>();

        for (final String requiredParam : loc.getParams()) {
            if (!result.containsKey(requiredParam)) {
                missingParams.add(requiredParam);
            }
        }
        if (!missingParams.isEmpty()) {
            LOGGER.error("Some parameters are missing for localization \"" + loc.getName()
                    + "\". Missing parameters: " + missingParams);
        }
        
        return result;
    }

    private void cacheLocalizationFiles() {
        LOGGER.info("Localization file caching is commencing...");
        final List<Path> locDirs = dao.listLocalizationDirs().stream()
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
                        + priorityCodesWithNoDir.toString(), null);
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
                        final Set<String> paramNames = textUtil.getParamNames(content);
                        
                        if (!paramNames.isEmpty()) {
                            LOGGER.trace("Localization " + key + " has custom parameters "
                                    + "that will need to be injected later.");
                            newLocalization = new Localization(key, content, paramNames, true);
                        } else {
                            LOGGER.trace("Localization " + key + " does not have any custom "
                                    + "parameters. Parsing markers now...");
                            final List<MessageEntity> entities =
                                    textUtil.getEntities(content);
                            newLocalization = new Localization(key, textUtil.removeMarkers(content),
                                    Set.of(), false);
                            newLocalization.setEntities(entities);
                        }
                        LOGGER.trace("Saving localization data...");
                        localizationRepository.save(newLocalization);
                    }
                    LOGGER.trace("Localization data from file " + locFile + " has been cached.");
                } catch (TaggedStringInterpretationException e) {
                    throw new LocalizationLoadingException("Unable to parse file " + locFile,
                            null, e);
                }
            }
        }
        LOGGER.info("Localization files cached successfuly.");
    }

    private Localization setUpLocalization(Localization localization, String injectedData) {
        localization.setEntities(textUtil.getEntities(injectedData));
        localization.setData(textUtil.removeMarkers(injectedData));
        LOGGER.trace("Entities set up.");
        return localization;
    }

    private Localization findAvailableLocalization(String name, String preferableLanguageCode, List<String> customLangPriority) {
        Optional<Localization> potentialLoc = localizationRepository.find(name + "_" + preferableLanguageCode);
        
        if (potentialLoc.isPresent()) {
            LOGGER.trace("Localization " + name + " for prefered code " + preferableLanguageCode + " is available.");
            return potentialLoc.get();
        }

        if (!customLangPriority.isEmpty()) {
            LOGGER.trace("Localization " + name + " for prefered code " + preferableLanguageCode
                    + " is not available. Looking through the custom language code priority list: " + customLangPriority + "...");
            for (String code : customLangPriority) {
                if (!code.equals(preferableLanguageCode)) {
                    potentialLoc = localizationRepository.find(name + "_" + code);
                    if (potentialLoc.isPresent()) {
                        LOGGER.trace("Localization " + name + " found for code " + code + ".");
                        return potentialLoc.get();
                    }
                }
            }
        }

        LOGGER.trace("Localization " + name + " for languages in the custom list"
                + " is not available. Looking over the application language code priority list...");
        for (String code : languagePriority) {
            if (!code.equals(preferableLanguageCode)) {
                potentialLoc = localizationRepository.find(name + "_" + code);
                if (potentialLoc.isPresent()) {
                    LOGGER.trace("Localization " + name + " found for code " + code + ".");
                    return potentialLoc.get();
                }
            }
        }
        LOGGER.warn("No localization with name " + name + " was found. The name will be sent instead.");
        return new Localization(name);
    }
}
