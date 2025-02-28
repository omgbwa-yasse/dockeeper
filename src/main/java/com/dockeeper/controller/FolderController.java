package com.dockeeper.controller;

import com.dockeeper.model.Folder;
import com.dockeeper.model.User;
import com.dockeeper.service.FolderService;
import com.dockeeper.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping({"/", "/folders"})
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    @GetMapping
    public String listFolders(@RequestParam(required = false) Long parentId, Model model) {
        List<Folder> folders;
        Folder currentFolder = null;
        
        if (parentId != null) {
            currentFolder = folderService.findById(parentId);
            folders = folderService.findByParentId(parentId);
        } else {
            folders = folderService.findRootFolders();
        }
        
        model.addAttribute("folders", folders);
        model.addAttribute("currentFolder", currentFolder);
        return "folders/index";
    }

    @GetMapping("/new")
    public String showCreateForm(@RequestParam(required = false) Long parentId, Model model) {
        Folder folder = new Folder();
        
        if (parentId != null) {
            Folder parentFolder = folderService.findById(parentId);
            folder.setParent(parentFolder);
        }
        
        model.addAttribute("folder", folder);
        model.addAttribute("parentFolders", folderService.findAllFolders());
        return "folders/create";
    }

    @PostMapping("/new")
    public String createFolder(@Valid @ModelAttribute("folder") Folder folder,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "folders/create";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        folder.setCreatedBy(currentUser);
        
        folderService.createFolder(folder);
        redirectAttributes.addFlashAttribute("successMessage", "Folder created successfully");
        
        if (folder.getParent() != null) {
            return "redirect:/folders?parentId=" + folder.getParent().getId();
        }
        return "redirect:/folders";
    }

    @GetMapping("/{id}")
    public String viewFolder(@PathVariable Long id, Model model) {
        Folder folder = folderService.findById(id);
        model.addAttribute("folder", folder);
        model.addAttribute("subfolders", folderService.findByParentId(id));
        model.addAttribute("documents", folder.getDocuments());
        return "folders/view";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Folder folder = folderService.findById(id);
        model.addAttribute("folder", folder);
        model.addAttribute("parentFolders", folderService.findAllExceptSelfAndChildren(id));
        return "folders/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateFolder(@PathVariable Long id,
                             @Valid @ModelAttribute("folder") Folder folder,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "folders/edit";
        }
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        folder.setUpdatedBy(currentUser);
        
        folderService.updateFolder(id, folder);
        redirectAttributes.addFlashAttribute("successMessage", "Folder updated successfully");
        
        if (folder.getParent() != null) {
            return "redirect:/folders?parentId=" + folder.getParent().getId();
        }
        return "redirect:/folders";
    }

    @PostMapping("/{id}/delete")
    public String deleteFolder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Folder folder = folderService.findById(id);
        Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userService.findByUsername(auth.getName());
        
        folderService.softDeleteFolder(id, currentUser);
        redirectAttributes.addFlashAttribute("successMessage", "Folder deleted successfully");
        
        if (parentId != null) {
            return "redirect:/folders?parentId=" + parentId;
        }
        return "redirect:/folders";
    }

    @GetMapping("/search")
    public String searchFolders(@RequestParam String query, Model model) {
        model.addAttribute("folders", folderService.searchFolders(query));
        model.addAttribute("searchQuery", query);
        return "folders/search";
    }
}