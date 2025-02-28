package com.dockeeper.service;

import com.dockeeper.model.Folder;
import com.dockeeper.model.User;
import com.dockeeper.repository.FolderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    public List<Folder> findAllFolders() {
        return folderRepository.findByDeletedAtIsNull();
    }

    public Folder findById(Long id) {
        return folderRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Folder not found with id: " + id));
    }

    public List<Folder> findRootFolders() {
        return folderRepository.findByParentIsNullAndDeletedAtIsNull();
    }

    public List<Folder> findByParentId(Long parentId) {
        return folderRepository.findByParentIdAndDeletedAtIsNull(parentId);
    }

    public List<Folder> findAllExceptSelfAndChildren(Long folderId) {
        Folder currentFolder = findById(folderId);
        List<Long> excludedIds = new ArrayList<>();
        excludedIds.add(folderId);
        
        // Add all descendants' ids
        addDescendantIds(currentFolder, excludedIds);
        
        return folderRepository.findByIdNotInAndDeletedAtIsNull(excludedIds);
    }

    private void addDescendantIds(Folder folder, List<Long> ids) {
        if (CollectionUtils.isNotEmpty(folder.getSubfolders())) {
            for (Folder subfolder : folder.getSubfolders()) {
                if (subfolder.getId() != null) {
                    ids.add(subfolder.getId());
                    addDescendantIds(subfolder, ids);
                }
            }
        }
    }

    @Transactional
    public Folder createFolder(Folder folder) {
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Transactional
    public Folder updateFolder(Long id, Folder updatedFolder) {
        Folder existingFolder = findById(id);
        
        existingFolder.setName(updatedFolder.getName());
        existingFolder.setDescription(updatedFolder.getDescription());
        existingFolder.setParent(updatedFolder.getParent());
        existingFolder.setUpdatedBy(updatedFolder.getUpdatedBy());
        existingFolder.setUpdatedAt(LocalDateTime.now());
        
        return folderRepository.save(existingFolder);
    }

    @Transactional
    public void softDeleteFolder(Long id, User deletedBy) {
        Folder folder = findById(id);
        folder.setDeletedBy(deletedBy);
        folder.setDeletedAt(LocalDateTime.now());
        folderRepository.save(folder);
        
        // Soft delete all subfolders and their documents
        if (CollectionUtils.isNotEmpty(folder.getSubfolders())) {
            for (Folder subfolder : folder.getSubfolders()) {
                softDeleteFolder(subfolder.getId(), deletedBy);
            }
        }
    }

    public List<Folder> searchFolders(String query) {
        return folderRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(query);
    }
}