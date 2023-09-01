package com.example.demo.services;


import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.services.exceptions.UniqueConstraintViolationError;
import com.example.demo.services.exceptions.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class AuthenticationService implements UserDetailsService {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCK_TIME_DURATION_SECONDS = 5 * 60; // 5 minutes

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordService passwordService;
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Email not found: " + email));
    }

    public String register(RegisterDTO registerDTO) {
        try {
            User user = createUserBasedOnRole(registerDTO);
            userRepository.save(user);
            return tokenService.generateToken(user);
        } catch (DataIntegrityViolationException e) {
            throw new UniqueConstraintViolationError("user", "email");
        }
    }

    public String login(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());

        Authentication authentication = this.authenticationManager
                .authenticate(usernamePasswordAuthenticationToken);

        User user = (User) authentication.getPrincipal();

        return tokenService.generateToken(user);
    }

    private User createUserBasedOnRole(RegisterDTO registerDTO) {
        Role role = registerDTO.getRole();
        return switch (role) {
            case Customer -> Customer.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordService.hashPassword(registerDTO.getPassword()))
                    .role(Role.Customer)
                    .build();
            case Seller -> Seller.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordService.hashPassword(registerDTO.getPassword()))
                    .role(Role.Seller)
                    .build();
            case Admin -> Admin.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordService.hashPassword(registerDTO.getPassword()))
                    .role(Role.Admin)
                    .build();
        };
    }

    public void increaseFailedAttempts(User user) {
        int newFailAttempts = user.getFailedAttempt() + 1;
        userRepository.updateFailedAttempts(newFailAttempts, user.getEmail());
    }

    public void resetFailedAttempts(String email) {
        userRepository.updateFailedAttempts(0, email);
    }

    public void lock(User user) {
        user.setAccountNonLocked(false);
        user.setLockTime(LocalDateTime.now());

        userRepository.save(user);
    }

    public boolean isLockTimeExpired(User user) {
        LocalDateTime lockTime = user.getLockTime();
        LocalDateTime currentTime = LocalDateTime.now();

        if (lockTime.plusSeconds(LOCK_TIME_DURATION_SECONDS).isBefore(currentTime)) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);

            userRepository.save(user);

            return true;
        }

        return false;
    }

}
