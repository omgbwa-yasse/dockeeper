package com.dockeeper.repository;

import com.dockeeper.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByDeletedAtIsNull();
    
    Optional<Document> findByIdAndDeletedAtIsNull(Long id);
    
    List<Document> findByFolderIdAndDeletedAtIsNull(Long folderId);
    
    List<Document> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
    
    List<Document> findByCreatedByIdAndDeletedAtIsNull(Long userId);
}