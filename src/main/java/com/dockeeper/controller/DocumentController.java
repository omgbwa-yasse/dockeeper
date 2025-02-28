package com.dockeeper.controller;

import com.dockeeper.model.Document;
import com.dockeeper.model.Folder;
import com.dockeeper.model.User;
import com.dockeeper.service.DocumentService;
import com.dockeeper.service.FolderService;
import com.dockeeper.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final FolderService folderService;
    private final UserService userService;

    @GetMapping
    public String listDocuments(Model model) {
        model.addAttribute("documents", documentService.findAllDocuments());
        return "documents/index";
    }

    @GetMapping("/upload")
    public String showUploadForm(@RequestParam(required = false) Long folderId, Model model) {
        Document document = new Document();
        
        if (folderId != null) {
            Folder folder = folderService.findById(folderId);
            document.setFolder(folder);
        }
        
        model.addAttribute("document", document);
        model.addAttribute("folders", folderService.findAllFolders());
        return "documents/upload";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                               @RequestParam("folderId") Long folderId,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/documents/upload?folderId=" + folderId;
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userService.findByUsername(auth.getName());
            Folder folder = folderService.findById(folderId);
            
            Document document = new Document();
            document.setName(FilenameUtils.getName(file.getOriginalFilename()));
            document.setDescription(description);
            document.setMimeType(file.getContentType());
            document.setSize(file.getSize());
            document.setFolder(folder);
            document.setCreatedBy(currentUser);
            document.setOwner(currentUser);
            
            documentService.uploadDocument(document, file);
            
            redirectAttributes.addFlashAttribute("successMessage", "Document uploaded successfully");
            return "redirect:/folders/" + folderId;
            
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload document: " + e.getMessage());
            return "redirect:/documents/upload?folderId=" + folderId;
        }
    }

    @GetMapping("/{id}")
    public String viewDocument(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id);
        model.addAttribute("document", document);
        return "documents/view";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Document document = documentService.findById(id);
        Resource resource = documentService.loadDocumentAsResource(document);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, document.getMimeType())
                .body(resource);
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Document document = documentService.findById(id);
        model.addAttribute("document", document);
        model.addAttribute("folders", folderService.findAllFolders());
        return "documents/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateDocument(@PathVariable Long id,
                               @Valid @ModelAttribute("document") Document document,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "documents/edit";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        document.setUpdatedBy(currentUser);
        
        documentService.updateDocument(id, document);
        redirectAttributes.addFlashAttribute("successMessage", "Document updated successfully");
        return "redirect:/folders/" + document.getFolder().getId();
    }

    @PostMapping("/{id}/delete")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Document document = documentService.findById(id);
        Long folderId = document.getFolder().getId();
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        
        documentService.softDeleteDocument(id, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Document deleted successfully");
        return "redirect:/folders/" + folderId;
    }

    @GetMapping("/search")
    public String searchDocuments(@RequestParam String query, Model model) {
        model.addAttribute("documents", documentService.searchDocuments(query));
        model.addAttribute("searchQuery", query);
        return "documents/search";
    }
}