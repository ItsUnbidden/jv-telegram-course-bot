package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.UnableToReadTextFileException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalizationDaoImpl implements LocalizationDao {
    @Override
    @NonNull
    public String getText(@NonNull Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UnableToReadTextFileException("File reading on path " + path
                    + " is obstructed.", e);
        }
    }

    @Override
    public boolean exists(@NonNull Path path) {
        return Files.exists(path);
    }
}
