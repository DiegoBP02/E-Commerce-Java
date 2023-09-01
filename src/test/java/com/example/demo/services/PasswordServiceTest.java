package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordServiceTest extends ApplicationConfigTest {

    @Autowired
    private PasswordService passwordService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserRepository userRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private User user = TestDataBuilder.buildUser();
    private ChangePasswordDTO changePasswordDTO = TestDataBuilder.buildChangePasswordDTO();

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

        verify(passwordEncoder,times(1)).encode(user.getPassword());
    }

}