package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.stereotype.Service;

@Service
public class ArchiveReviewsDaoImpl implements ArchiveReviewsDao {
    private static final Path TEMP_DIR_PATH = Path.of(System.getProperty("user.dir")).resolve("temp/reviews");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(TEMP_DIR_PATH);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create a new directory in "
                    + TEMP_DIR_PATH + " for temp reviews files.", null, e);
        }
    }

    @Override
    public Path createTempFile(String name) {
        try {
            return Files.createTempFile(TEMP_DIR_PATH, name, null);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to create a new temp file in directory "
                    + TEMP_DIR_PATH + " for reviews.", null, e);
        }
    }

    @Override
    public Path write(Path path, String content) {
        try {
            return Files.write(path, content.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to append to the temp file by path "
                    + path, null, e);
        }
    }

    @Override
    public InputStream read(Path path) {
        try {
            return Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to read the temp file by path "
                    + path, null, e);
        }
    }

    @Override
    public void delete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to delete the temp file by path "
                    + path, null, e);
        }
    }
}
