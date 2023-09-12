package com.example.demo.services;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.ForgotPasswordDTO;
import com.example.demo.dtos.ResetPasswordDTO;
import com.example.demo.entities.ResetPasswordToken;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.ResetPasswordTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;

@Service
public class PasswordService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Lazy
    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResetPasswordTokenRepository resetPasswordTokenRepository;

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

    public void forgotPassword(HttpServletRequest request, ForgotPasswordDTO forgotPasswordDTO) {
        UUID token = createResetPasswordToken(forgotPasswordDTO.getEmail());
        String resetPasswordLink = getSiteURL(request) + "/reset-password?token=" + token;

        String subject = "Here's the link to reset your password";

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + resetPasswordLink + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or you have not made the request.</p>";

        emailService.sendEmail(forgotPasswordDTO.getEmail(), subject, content);
    }

    private String getSiteURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString();
        return siteURL.replace(request.getServletPath(), "") + "/password";
    }

    private UUID createResetPasswordToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Email not found: " + email));
        if (user.getResetPasswordToken() != null) {
            if (!user.getResetPasswordToken().isTokenExpired()) {
                throw new ResetEmailAlreadySentException();
            } else {
                resetPasswordTokenRepository.deleteById(user.getResetPasswordToken().getId());
            }
        }
        ResetPasswordToken resetPasswordToken = new ResetPasswordToken(user);
        user.setResetPasswordToken(resetPasswordToken);
        userRepository.save(user);
        return resetPasswordToken.getResetPasswordToken();
    }

    public void resetPassword(UUID token, ResetPasswordDTO resetPasswordDTO) {
        User user = userRepository.findByResetPasswordTokenResetPasswordToken(token)
                .orElseThrow(InvalidTokenException::new);

        if (user.getResetPasswordToken().isTokenExpired()) {
            throw new ResetPasswordTokenExpiredException();
        }

        changePasswordByUser(user, resetPasswordDTO.getPassword());
    }

    private void changePasswordByUser(User user, String newPassword) {
        String encodedPassword = hashPassword(newPassword);
        user.setPassword(encodedPassword);
        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

}
