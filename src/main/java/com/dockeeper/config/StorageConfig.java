package com.dockeeper.config;

import com.dockeeper.repository.DocumentRepository;
import com.dockeeper.service.DocumentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Bean
    public DocumentService documentService(DocumentRepository documentRepository) {
        return new DocumentService(documentRepository, uploadDir);
    }
}