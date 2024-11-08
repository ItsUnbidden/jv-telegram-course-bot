package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CertificateDaoImpl implements CertificateDao {
    @Value("${server.ssl.certificate}")
    private String certificatePathStr;

    private Path certificatePath;

    @PostConstruct
    public void init() {
        certificatePath = Path.of(certificatePathStr.replace("classpath:",
                System.getProperty("user.dir") + "/src/main/resources"));
    }

    @Override
    public InputStream readPublicKey() {
        try {
            if (Files.exists(certificatePath)) {
                return Files.newInputStream(certificatePath, StandardOpenOption.READ);
            }
            throw new FileDaoOperationException("Public certificate key does not exist by path "
                    + certificatePath, null);
        } catch (IOException e) {
            throw new FileDaoOperationException("Unable to read certificate public key by path "
                    + certificatePath, null, e);
        }
    }
}
