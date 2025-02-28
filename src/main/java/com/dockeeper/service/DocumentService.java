package com.dockeeper.service;

import com.dockeeper.model.Document;
import com.dockeeper.model.User;
import com.dockeeper.repository.DocumentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final Path fileStorageLocation;

    // Constructor to initialize file storage location
    public DocumentService(DocumentRepository documentRepository, String uploadDir) {
        this.documentRepository = documentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<Document> findAllDocuments() {
        return documentRepository.findByDeletedAtIsNull();
    }

    public Document findById(Long id) {
        return documentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Document not found with id: " + id));
    }

    public List<Document> findByFolderId(Long folderId) {
        return documentRepository.findByFolderIdAndDeletedAtIsNull(folderId);
    }

    @Transactional
    public Document uploadDocument(Document document, MultipartFile file) throws IOException {
        // Generate a unique filename
        String originalFilename = FilenameUtils.getName(file.getOriginalFilename());
        String extension = FilenameUtils.getExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + "." + extension;
        
        // Save file to storage
        Path targetLocation = this.fileStorageLocation.resolve(newFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Calculate checksum
        String checksum = DigestUtils.md5DigestAsHex(file.getInputStream());
        document.setFileChecksum(checksum);
        
        // Save document metadata to database
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        return documentRepository.save(document);
    }

    public Resource loadDocumentAsResource(Document document) {
        try {
            // Extract filename from the document
            String filename = document.getFileChecksum() + "." + 
                            FilenameUtils.getExtension(document.getName());
            
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if(resource.exists()) {
                return resource;
            } else {
                throw new EntityNotFoundException("File not found " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new EntityNotFoundException("File not found", ex);
        }
    }

    @Transactional
    public Document updateDocument(Long id, Document updatedDocument) {
        Document existingDocument = findById(id);
        
        existingDocument.setName(updatedDocument.getName());
        existingDocument.setDescription(updatedDocument.getDescription());
        existingDocument.setFolder(updatedDocument.getFolder());
        existingDocument.setUpdatedBy(updatedDocument.getUpdatedBy());
        existingDocument.setUpdatedAt(LocalDateTime.now());
        
        return documentRepository.save(existingDocument);
    }

    @Transactional
    public void softDeleteDocument(Long id, User deletedBy) {
        Document document = findById(id);
        document.setDeletedBy(deletedBy);
        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    public List<Document> searchDocuments(String query) {
        return documentRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(query);
    }
}