package com.unbidden.telegramcoursesbot.dao;

import com.unbidden.telegramcoursesbot.exception.FileDaoOperationException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CertificateDaoImpl implements CertificateDao {
    @Value("${server.ssl.certificate}")
    private String certificatePathStr;

    private Path certificatePath;

    @PostConstruct
    public void init() {
        try {
            final String certificateName = Path.of(certificatePathStr.replace("classpath:", ""))
                    .getFileName().toString();
            final URI certificateUri = getClass().getClassLoader()
                    .getResource(certificateName).toURI();
            
            if ("jar".equals(certificateUri.getScheme())) {
                FileSystem fileSystem = null;
                try {
                    fileSystem = FileSystems.newFileSystem(certificateUri,
                            Collections.emptyMap(), null);

                    certificatePath = fileSystem.getPath(certificateName);
                } catch (IOException e) {
                    throw new FileDaoOperationException("Unable to create a new file system "
                            + "from certificate uri", null);
                } finally {
                    if (fileSystem != null) {
                        try {
                            fileSystem.close();
                        } catch (IOException e) {
                            throw new FileDaoOperationException("Unable to close the file system",
                                null);
                        }
                    }
                }
            } else {
                certificatePath = Paths.get(certificateUri);
            }
        } catch (URISyntaxException e) {
            throw new FileDaoOperationException("Unable to create public certificate uri", null);
        }
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
