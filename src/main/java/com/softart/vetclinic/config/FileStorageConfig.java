package com.softart.vetclinic.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Getter
public class FileStorageConfig {

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    private Path basePath;

    @PostConstruct
    public void init() throws IOException {
        this.basePath = Paths.get(baseDir).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
    }
}
