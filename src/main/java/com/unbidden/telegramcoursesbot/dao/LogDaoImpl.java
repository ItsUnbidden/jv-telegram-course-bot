package com.unbidden.telegramcoursesbot.dao;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class LogDaoImpl implements LogDao {
    private static final String currentLogsFilePathStr = "logs/tcb-2024-10-11.log";

    private Path currentLogsFilePath;

    @PostConstruct
    public void init() {
        currentLogsFilePath = Path.of(System.getProperty("user.dir"))
                .resolve(currentLogsFilePathStr);
    }

    @Override
    @NonNull
    public InputStream readCurrentLogFile() {
        try {
            if (Files.exists(currentLogsFilePath)) {
                return Files.newInputStream(currentLogsFilePath, StandardOpenOption.READ);
            }
            throw new RuntimeException("Current log file does not exist.");
        } catch (IOException e) {
            throw new RuntimeException("Unable to read log file.");
        }
    }
}
