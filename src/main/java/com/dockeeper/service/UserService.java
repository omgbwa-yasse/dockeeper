package com.dockeeper.service;

import com.dockeeper.model.User;
import com.dockeeper.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User createUser(User user) {
        // Encode password
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = findById(id);
        
        // Update fields
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setEmail(updatedUser.getEmail());
        
        // Only update password if it's not null/empty
        if (updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isEmpty()) {
            existingUser.setPasswordHash(passwordEncoder.encode(updatedUser.getPasswordHash()));
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existingUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id);
        user.setEnabled(false);
        userRepository.save(user);
    }

    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    // Generate password reset token
                    String token = UUID.randomUUID().toString();
                    // Store token in database with expiration (implementation detail)
                    savePasswordResetToken(user, token);
                    
                    // Send email
                    String resetUrl = "http://localhost:8080/reset-password?token=" + token;
                    String emailBody = "Click the link to reset your password: " + resetUrl;
                    emailService.sendEmail(user.getEmail(), "Password Reset", emailBody);
                    
                    return true;
                })
                .orElse(false);
    }

    // Helper method to store token
    private void savePasswordResetToken(User user, String token) {
        // Implementation details would depend on how you store tokens
        // This could be in a separate table or in the user table
    }

    public boolean validatePasswordResetToken(String token) {
        // Implementation would check if token exists and is not expired
        return true; // Placeholder implementation
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        // Implementation would find user by token and update password
        // Here's a placeholder implementation
        return true;
    }
}