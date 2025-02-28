package com.dockeeper.controller;

import com.dockeeper.model.User;
import com.dockeeper.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        // Check if username or email already exists
        if (userService.existsByUsername(user.getUsername())) {
            result.rejectValue("username", "error.user", "Username already exists");
            return "auth/register";
        }
        
        if (userService.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error.user", "Email already exists");
            return "auth/register";
        }
        
        // Create user
        userService.createUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
        return "redirect:/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@ModelAttribute("email") String email,
                                      RedirectAttributes redirectAttributes) {
        boolean emailSent = userService.sendPasswordResetEmail(email);
        if (emailSent) {
            redirectAttributes.addFlashAttribute("successMessage", 
                "Password reset instructions have been sent to your email.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Email not found or unable to send reset instructions.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@ModelAttribute("token") String token, Model model) {
        boolean validToken = userService.validatePasswordResetToken(token);
        if (!validToken) {
            model.addAttribute("errorMessage", "Invalid or expired token");
            return "auth/invalid-token";
        }
        
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@ModelAttribute("token") String token,
                                     @ModelAttribute("password") String password,
                                     RedirectAttributes redirectAttributes) {
        boolean resetSuccess = userService.resetPassword(token, password);
        if (resetSuccess) {
            redirectAttributes.addFlashAttribute("successMessage", 
                "Password has been reset successfully. Please log in with your new password.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Failed to reset password. Please try again.");
            return "redirect:/reset-password?token=" + token;
        }
    }

    @GetMapping("/access-denied")
    public String showAccessDenied() {
        return "auth/access-denied";
    }

    @PostMapping("/update-last-login")
    public void updateLastLogin(@ModelAttribute("username") String username) {
        User user = userService.findByUsername(username);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            userService.updateUser(user.getId(), user);
        }
    }
}