package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.ForgotPasswordDTO;
import com.example.demo.dtos.ResetPasswordDTO;
import com.example.demo.entities.ResetPasswordToken;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.ResetPasswordTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordServiceTest extends ApplicationConfigTest {

    @Autowired
    private PasswordService passwordService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private ResetPasswordTokenRepository resetPasswordTokenRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private User user = TestDataBuilder.buildUserWithId();
    private ChangePasswordDTO changePasswordDTO = TestDataBuilder.buildChangePasswordDTO();
    private ForgotPasswordDTO forgotPasswordDTO = TestDataBuilder.buildForgotPasswordDTO();
    private ResetPasswordDTO resetPasswordDTO = TestDataBuilder.buildResetPasswordDTO();
    private ResetPasswordToken resetPasswordTokenMock = mock(ResetPasswordToken.class);
    private String URL = "URL";

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenChangePasswordDTO_whenChangePassword_thenChangeUserPassword() {
        String hashedPassword = "hashedPassword";
        String userOldPassword = user.getPassword();
        when(passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword()))
                .thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(passwordEncoder.encode(changePasswordDTO.getNewPassword()))
                .thenReturn(hashedPassword);

        passwordService.changePassword(changePasswordDTO);

        assertEquals(hashedPassword, user.getPassword());

        verify(passwordEncoder, times(1))
                .matches(changePasswordDTO.getOldPassword(), userOldPassword);
        verify(userRepository, times(1)).save(user);
        verify(passwordEncoder, times(1))
                .encode(changePasswordDTO.getNewPassword());
        verifyAuthentication();
    }

    @Test
    void givenOldPasswordDoesNotMatches_whenChangePassword_thenThrowInvalidOldPasswordException() {
        when(passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword()))
                .thenReturn(false);

        assertThrows(InvalidOldPasswordException.class,
                () -> passwordService.changePassword(changePasswordDTO));

        verify(passwordEncoder, times(1))
                .matches(changePasswordDTO.getOldPassword(), user.getPassword());
        verifyAuthentication();
    }

    @Test
    void givenPassword_whenHashPassword_thenReturnHashedPassword() {
        String hashedPassword = "hashedPassword";
        when(passwordEncoder.encode(user.getPassword())).thenReturn(hashedPassword);

        String result = passwordService.hashPassword(user.getPassword());

        assertEquals(hashedPassword, result);

        verify(passwordEncoder, times(1)).encode(user.getPassword());
    }

    @Test
    void givenNoUser_whenForgotPassword_thenThrowUserNotFoundException() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThrows(UserNotFoundException.class, () ->
                passwordService.forgotPassword(request, forgotPasswordDTO)
        );

        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(userRepository, never()).save(user);
        verify(emailService, never())
                .sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void givenUserDoesNotHaveAResetPasswordToken_whenForgotPassword_thenGenerateTokenAndSendEmail() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getServletPath()).thenReturn("/path");

        passwordService.forgotPassword(request, forgotPasswordDTO);

        assertNotNull(user.getResetPasswordToken());

        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1))
                .sendEmail(eq(forgotPasswordDTO.getEmail()), anyString(), anyString());
    }

    @Test
    void givenUserAlreadyHaveANotExpiredResetPasswordToken_whenForgotPassword_thenThrowResetEmailAlreadySentException() {
        when(resetPasswordTokenMock.isTokenExpired()).thenReturn(false);
        user.setResetPasswordToken(resetPasswordTokenMock);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getServletPath()).thenReturn("/path");

        assertThrows(ResetEmailAlreadySentException.class,
                () -> passwordService.forgotPassword(request, forgotPasswordDTO));

        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void givenUserAlreadyHaveAExpiredResetPasswordToken_whenForgotPassword_thenGenerateTokenAndSendEmail() {
        when(resetPasswordTokenMock.isTokenExpired()).thenReturn(true);
        when(resetPasswordTokenMock.getId()).thenReturn(UUID.randomUUID());
        user.setResetPasswordToken(resetPasswordTokenMock);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getServletPath()).thenReturn("/path");

        passwordService.forgotPassword(request, forgotPasswordDTO);

        assertNotNull(user.getResetPasswordToken());

        verify(userRepository, times(1)).findByEmail(user.getEmail());
        verify(resetPasswordTokenRepository, times(1))
                .deleteById(resetPasswordTokenMock.getId());
        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1))
                .sendEmail(eq(forgotPasswordDTO.getEmail()), anyString(), anyString());
    }

    @Test
    void givenTokenAndResetPasswordDTO_whenResetPassword_thenChangePasswordAndNullifyUserPasswordToken() {
        UUID token = UUID.randomUUID();
        String hashedPassword = "hashedPassword";
        when(resetPasswordTokenMock.isTokenExpired()).thenReturn(false);
        user.setResetPasswordToken(resetPasswordTokenMock);
        when(userRepository.findByResetPasswordTokenResetPasswordToken(token))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.encode(resetPasswordDTO.getPassword())).thenReturn(hashedPassword);

        passwordService.resetPassword(token, resetPasswordDTO);

        assertEquals(hashedPassword, user.getPassword());
        assertNull(user.getResetPasswordToken());

        verify(userRepository, times(1))
                .findByResetPasswordTokenResetPasswordToken(token);
        verify(passwordEncoder, times(1)).encode(resetPasswordDTO.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenNoUser_whenResetPassword_thenThrowInvalidTokenException() {
        UUID token = UUID.randomUUID();
        when(userRepository.findByResetPasswordTokenResetPasswordToken(token))
                .thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class,
                () -> passwordService.resetPassword(token, resetPasswordDTO));

        verify(userRepository, times(1))
                .findByResetPasswordTokenResetPasswordToken(token);
        verify(passwordEncoder, never()).encode(resetPasswordDTO.getPassword());
        verify(userRepository, never()).save(user);
    }

    @Test
    void givenResetPasswordTokenIsExpired_whenResetPassword_thenThrowResetPasswordTokenExpired() {
        when(resetPasswordTokenMock.isTokenExpired()).thenReturn(true);
        user.setResetPasswordToken(resetPasswordTokenMock);
        UUID token = UUID.randomUUID();
        when(userRepository.findByResetPasswordTokenResetPasswordToken(token))
                .thenReturn(Optional.of(user));

        assertThrows(ResetPasswordTokenExpiredException.class,
                () -> passwordService.resetPassword(token, resetPasswordDTO));

        verify(userRepository, times(1))
                .findByResetPasswordTokenResetPasswordToken(token);
        verify(passwordEncoder, never()).encode(resetPasswordDTO.getPassword());
        verify(userRepository, never()).save(user);
    }

}