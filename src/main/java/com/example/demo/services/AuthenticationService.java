package com.example.demo.services;


import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.dtos.UserLoginResponseDTO;
import com.example.demo.entities.ConfirmationToken;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ConfirmationTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;

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
    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;
    @Autowired
    private EmailService emailService;

    @Override
    public User loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Email not found: " + email));
    }

    public UserLoginResponseDTO register(RegisterDTO registerDTO) {
        try {
            User user = createUserBasedOnRole(registerDTO);
            userRepository.save(user);
            String token = tokenService.generateToken(user);

            return UserLoginResponseDTO.builder()
                    .token(token)
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .role(registerDTO.getRole())
                    .build();
        } catch (DataIntegrityViolationException e) {
            throw new UniqueConstraintViolationError("user", "email");
        }
    }

    public UserLoginResponseDTO login(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());

        Authentication authentication = this.authenticationManager
                .authenticate(usernamePasswordAuthenticationToken);

        User user = (User) authentication.getPrincipal();
        String token = tokenService.generateToken(user);

        return UserLoginResponseDTO.builder()
                .token(token)
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private User createUserBasedOnRole(RegisterDTO registerDTO) {
        Role role = registerDTO.getRole();
        return switch (role) {
            case Customer -> Customer.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordService.hashPassword(registerDTO.getPassword()))
                    .build();
            case Seller -> Seller.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordService.hashPassword(registerDTO.getPassword()))
                    .build();
            case Admin -> throw new InvalidRoleException();
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
        user.setLockTime(Instant.now());

        userRepository.save(user);
    }

    public boolean isLockTimeExpired(User user) {
        Instant lockTime = user.getLockTime();
        Instant currentTime = Instant.now();

        if (lockTime.plusSeconds(LOCK_TIME_DURATION_SECONDS).isBefore(currentTime)) {
            user.setAccountNonLocked(true);
            user.setLockTime(null);
            user.setFailedAttempt(0);

            userRepository.save(user);

            return true;
        }

        return false;
    }

    public void confirmationRequest(HttpServletRequest request) {
        User user = getCurrentUser();

        if (user.isEnabled()) {
            throw new UserAlreadyEnabledException();
        }

        ConfirmationToken confirmationToken = confirmationTokenRepository.findByUser(user);

        if (confirmationToken != null) {
            if (confirmationToken.isTokenExpired()) {
                confirmationToken.resetToken();
            } else {
                throw new ConfirmationTokenAlreadyExistsException(confirmationToken.getTimeUntilExpiration());
            }
        } else {
            confirmationToken = new ConfirmationToken(user);
        }

        confirmationTokenRepository.save(confirmationToken);

        String subject = "Complete account confirmation!";
        String tokenLink = getSiteURL(request) + "/confirm-account?token=" + confirmationToken.getConfirmationToken();
        String content = "<p>Hello,</p>"
                + "<p>To confirm your account, please click in the link below: </p>"
                + "<p><a href=\"" + tokenLink + "\">Confirm my account</a></p>";

        emailService.sendEmail(user.getEmail(), subject, content);
    }

    public void confirmAccount(HttpServletRequest request, UUID token) {
        ConfirmationToken confirmationToken =
                confirmationTokenRepository.findByConfirmationToken(token)
                        .orElseThrow(() -> new EntityNotFoundException("Token not found"));

        User user = userRepository.findByEmail(confirmationToken.getUser().getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found for confirmation token"));

        if (confirmationToken.isTokenExpired()) {
            confirmationToken.resetToken();
            String subject = "New confirmation token";
            String newTokenLink = getSiteURL(request) + "/confirm-account?token=" + confirmationToken.getConfirmationToken();
            String content = "<p>Hello,</p>"
                    + "<p>Your previous confirmation token has expired.</p>"
                    + "Here is a new confirmation token link: "
                    + "<p><a href=\"" + newTokenLink + "\">Confirm my account</a></p>";

            emailService.sendEmail(user.getEmail(), subject, content);
            throw new ConfirmationTokenExpiredException();
        }

        user.setEnabled(true);
        userRepository.save(user);

        confirmationTokenRepository.deleteById(confirmationToken.getId());
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "") + "/auth";
    }
}
