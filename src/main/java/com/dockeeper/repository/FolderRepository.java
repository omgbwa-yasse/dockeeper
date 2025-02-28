package com.dockeeper.repository;

import com.dockeeper.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    
    List<Folder> findByDeletedAtIsNull();
    
    Optional<Folder> findByIdAndDeletedAtIsNull(Long id);
    
    List<Folder> findByParentIsNullAndDeletedAtIsNull();
    
    List<Folder> findByParentIdAndDeletedAtIsNull(Long parentId);
    
    List<Folder> findByNameContainingIgnoreCaseAndDeletedAtIsNull(String name);
    
    List<Folder> findByIdNotInAndDeletedAtIsNull(List<Long> ids);
}