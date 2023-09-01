package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.UniqueConstraintViolationError;
import com.example.demo.services.exceptions.UserNotFoundException;
import com.example.demo.utils.TestDataBuilder;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest extends ApplicationConfigTest {

    @Autowired
    private AuthenticationService authenticationService;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private TokenService tokenService;
    @MockBean
    private UserRepository userRepository;

    private User user = TestDataBuilder.buildUser();
    private RegisterDTO registerDTO = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDTO = TestDataBuilder.buildLoginDTO();
    private String token = "token";

    @Test
    void givenUser_whenLoadUserByUsername_thenReturnUser() {
        String email = user.getEmail();
        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        UserDetails result = authenticationService.loadUserByUsername(email);

        assertEquals(user, result);

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void givenNoUser_whenLoadUserByUsername_thenThrowUsernameNotFoundException() {
        String email = user.getEmail();

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                authenticationService.loadUserByUsername(email));

        assertEquals("Email not found: " + email, exception.getMessage());

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void givenValidSeller_whenRegister_thenReturnToken() {
        registerDTO.setRole(Role.Seller);
        when(tokenService.generateToken(any(User.class))).thenReturn(token);

        String result = authenticationService.register(registerDTO);

        assertEquals(token, result);

        verify(userRepository, times(1)).save(any(Seller.class));
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    void givenValidCustomer_whenRegister_thenReturnToken() {
        registerDTO.setRole(Role.Customer);
        when(tokenService.generateToken(any(User.class))).thenReturn(token);

        String result = authenticationService.register(registerDTO);

        assertEquals(token, result);

        verify(userRepository, times(1)).save(any(Customer.class));
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    void givenValidAdmin_whenRegister_thenReturnToken() {
        registerDTO.setRole(Role.Admin);
        when(tokenService.generateToken(any(User.class))).thenReturn(token);

        String result = authenticationService.register(registerDTO);

        assertEquals(token, result);

        verify(userRepository, times(1)).save(any(Admin.class));
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    void givenUserAlreadyExists_whenRegister_thenThrowUniqueConstraintViolationError() {
        when(userRepository.save(any(User.class)))
                .thenThrow(DataIntegrityViolationException.class);

        assertThrows(UniqueConstraintViolationError.class, () -> {
            authenticationService.register(registerDTO);
        });

        verify(userRepository, times(1)).save(any(User.class));
        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    void givenUser_whenLogin_thenReturnToken() {
        String token = "token";
        Authentication authenticate = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticate);
        when(authenticate.getPrincipal()).thenReturn(user);
        when(tokenService.generateToken(any(User.class))).thenReturn(token);

        String result = authenticationService.login(loginDTO);

        assertEquals(token, result);

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(authenticate, times(1)).getPrincipal();
        verify(tokenService, times(1)).generateToken(any(User.class));
    }

    @Test
    void givenUser_whenIncreaseFailedAttempts_thenIncreaseFailedAttemptsByOne() {
        authenticationService.increaseFailedAttempts(user);

        verify(userRepository, times(1))
                .updateFailedAttempts(user.getFailedAttempt() + 1, user.getEmail());
    }

    @Test
    void givenEmail_whenResetFailedAttempts_thenUpdateFailedAttemptsTo0() {
        authenticationService.resetFailedAttempts(user.getEmail());

        verify(userRepository, times(1))
                .updateFailedAttempts(0, user.getEmail());
    }

    @Test
    void givenUser_whenLock_thenSetAccountNonLockedToFalseAndSetLockTimeToCurrentTimeAndSaveUser() {
        TemporalUnitOffset temporalUnitOffset
                = new TemporalUnitWithinOffset(5, ChronoUnit.MINUTES);

        authenticationService.lock(user);

        assertFalse(user.isAccountNonLocked());
        assertThat(user.getLockTime()).isCloseTo(LocalDateTime.now(), temporalUnitOffset);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenUserAndLockTimeIsExpired_whenIsLockTimeExpired_thenReturnTrueAndUnlockUser(){
        user.setLockTime(LocalDateTime.now().minusYears(1));

        boolean result = authenticationService.isLockTimeExpired(user);
        assertTrue(result);
        assertTrue(user.isAccountNonLocked());
        assertNull(user.getLockTime());
        assertEquals(0, user.getFailedAttempt());

        verify(userRepository,times(1)).save(user);
    }

    @Test
    void givenUserAndLockTimeIsNotExpired_whenIsLockTimeExpired_thenReturnFalseAndDoesNotUnlockUser(){
        user.setLockTime(LocalDateTime.now());
        user.setAccountNonLocked(false);

        boolean result = authenticationService.isLockTimeExpired(user);
        assertFalse(result);
        assertFalse(user.isAccountNonLocked());

        verify(userRepository,never()).save(user);
    }
}