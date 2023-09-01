package com.example.demo.services;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        User user = getCurrentUser();

        if (!isPasswordMatch(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new InvalidOldPasswordException();
        }

        changeUserPassword(user, changePasswordDTO.getNewPassword());
    }

    private void changeUserPassword(User user, String newPassword) {
        user.setPassword(hashPassword(newPassword));
        userRepository.save(user);
    }

    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    private boolean isPasswordMatch(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
