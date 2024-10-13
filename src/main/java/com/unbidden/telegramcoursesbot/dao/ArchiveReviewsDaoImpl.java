package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ArchiveReviewsDaoImpl implements ArchiveReviewsDao {
    @Value("${telegram.bot.reviews.archive.temp.path}")
    private String tempDirectoryPath;

    private Path tempPath;

    @PostConstruct
    public void init() {
        tempPath = Path.of(System.getProperty("user.dir")).resolve(tempDirectoryPath);
        try {
            Files.createDirectories(tempPath);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create a new directory in "
                    + tempPath + " for temp reviews files.", e);
        }
    }

    @Override
    @NonNull
    public Path createTempFile(@NonNull String name) {
        try {
            return Files.createTempFile(tempPath, name, null);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create a new temp file in directory "
                    + tempPath + " for reviews.", e);
        }
    }

    @Override
    @NonNull
    public Path write(@NonNull Path path, @NonNull String content) {
        try {
            return Files.write(path, content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to append to the temp file by path "
                    + path, e);
        }
    }

    @Override
    @NonNull
    public InputStream read(@NonNull Path path) {
        try {
            return Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to read the temp file by path "
                    + path, e);
        }
    }

    @Override
    public void delete(@NonNull Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to delete the temp file by path "
                    + path, e);
        }
    }
}
