package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalizationDaoImpl implements LocalizationDao {
    private static final Logger LOGGER = LogManager.getLogger(LocalizationDaoImpl.class);

    @Value("${telegram.bot.message.text.format}")
    private String fileFormat;

    @Value("${telegram.bot.message.text.path}")
    private String pathStr;

    private Path localizationFolderPath;
   
    @PostConstruct
    public void init() {
        localizationFolderPath = Path.of(System.getProperty("user.dir")).resolve(pathStr);
        LOGGER.info("Localization folder is set to " + localizationFolderPath.toString() + ".");
        if (!exists(localizationFolderPath)) {
            LOGGER.info("Folder does not exist. Creating...");
            createDir();
        }
    }

    @Override
    @NonNull
    public Path createDir() {
        try {
            return Files.createDirectories(localizationFolderPath);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create default localization "
                    + "directory on path " + localizationFolderPath, null, e);
        }
    }

    @Override
    @NonNull
    public String getText(@NonNull Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileDaoOperationException("File reading on path " + path
                    + " is obstructed.", null, e);
        }
    }

    @Override
    public boolean exists(@NonNull Path path) {
        return Files.exists(path);
    }

    @Override
    @NonNull
    public List<Path> list(@NonNull Path path) {
        try {
            return Files.list(path).toList();
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to list entities on path " + path,
                    null, e);
        }
    }

    @Override
    @NonNull
    public List<Path> listLocalizationDirs() {
        try {
            return Files.list(localizationFolderPath).toList();
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to list localization directories on path "
                    + localizationFolderPath, null, e);
        }
    }
}
