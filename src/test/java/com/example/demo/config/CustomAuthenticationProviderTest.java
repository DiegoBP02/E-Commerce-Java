package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.user.User;
import com.example.demo.services.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomAuthenticationProviderTest extends ApplicationConfigTest {

    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthenticationService authenticationService;
    Authentication authentication;

    User user = mock(User.class);

    @BeforeEach
    void setUp() {
        when(user.getEmail()).thenReturn("email");
        when(user.getPassword()).thenReturn("password");

        authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
    }

    @Test
    @DisplayName("given user and valid credentials, when authenticate, " +
            "then reset user failed attempts and return UsernamePasswordAuthenticationToken")
    void authenticate_validUser() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        when(user.isAccountNonLocked()).thenReturn(true);
        when(user.getFailedAttempt()).thenReturn(0);

        Authentication result = customAuthenticationProvider.authenticate(authentication);

        assertTrue(result.isAuthenticated());
        assertEquals(user, result.getPrincipal());

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).getFailedAttempt();
        verify(user, times(1)).isAccountNonLocked();
        verify(authenticationService, never()).resetFailedAttempts(user.getEmail());
    }

    @Test
    @DisplayName("given user and valid credentials and failed attempts, when authenticate, " +
            "then reset user failed attempts and return UsernamePasswordAuthenticationToken")
    void authenticate_validUserFailedAttempts() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(true);
        when(user.isAccountNonLocked()).thenReturn(true);
        when(user.getFailedAttempt()).thenReturn(1);

        Authentication result = customAuthenticationProvider.authenticate(authentication);

        assertTrue(result.isAuthenticated());
        assertEquals(user, result.getPrincipal());

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).isAccountNonLocked();
        verify(user, times(1)).getFailedAttempt();
        verify(authenticationService, times(1)).resetFailedAttempts(user.getEmail());
    }

    @Test
    @DisplayName("given user is non locked and failed attempts are smaller than " +
            "MAX_FAILED_ATTEMPTS, when authenticate, then increase user failed attempts and" +
            "throw BadCredentialsException")
    void authenticate_invalidUser() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(false);
        when(user.isAccountNonLocked()).thenReturn(true);
        when(user.getFailedAttempt()).thenReturn(0);

        BadCredentialsException badCredentialsException =
                assertThrows(BadCredentialsException.class, () ->
                        customAuthenticationProvider.authenticate(authentication));
        assertEquals(badCredentialsException.getMessage(), "Wrong password or username.");

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).isAccountNonLocked();
        verify(user, times(1)).getFailedAttempt();
        verify(authenticationService, times(1)).increaseFailedAttempts(user);
    }

    @Test
    @DisplayName("given user is non locked and failed attempts reach MAX_FAILED_ATTEMPTS," +
            "when authenticate, then lock user account and throw LockedException")
    void authenticate_invalidUserMaxFailedAttempts() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(false);
        when(user.isAccountNonLocked()).thenReturn(true);
        when(user.getFailedAttempt()).thenReturn(AuthenticationService.MAX_FAILED_ATTEMPTS - 1);

        LockedException lockedException =
                assertThrows(LockedException.class, () ->
                        customAuthenticationProvider.authenticate(authentication));
        assertEquals(lockedException.getMessage(),
                "Your account has been locked due to 3 failed login attempts."
                        + " It will be unlocked after 5 minutes.");

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).isAccountNonLocked();
        verify(user, times(1)).getFailedAttempt();
        verify(authenticationService, never()).increaseFailedAttempts(user);
        verify(authenticationService, times(1)).lock(user);
    }

    @Test
    @DisplayName("given user is locked and lock time expired, when authenticate," +
            "then throw LockedException warning user account has been unlocked")
    void authenticate_userLockedLockTimeExpired() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(false);
        when(user.isAccountNonLocked()).thenReturn(false);
        when(authenticationService.isLockTimeExpired(user)).thenReturn(true);

        LockedException lockedException =
                assertThrows(LockedException.class, () ->
                        customAuthenticationProvider.authenticate(authentication));
        assertEquals(lockedException.getMessage(),
                "Your account has been unlocked. Please try to login again.");

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).isAccountNonLocked();
        verify(authenticationService, times(1)).isLockTimeExpired(user);
    }

    @Test
    @DisplayName("given user is locked and lock time has not expired, when authenticate," +
            "then throw LockedException warning that the account has been locked")
    void authenticate_userLocked() {
        when(authenticationService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordEncoder.matches(user.getPassword(), user.getPassword())).thenReturn(false);
        when(user.isAccountNonLocked()).thenReturn(false);
        when(authenticationService.isLockTimeExpired(user)).thenReturn(false);

        LockedException lockedException =
                assertThrows(LockedException.class, () ->
                        customAuthenticationProvider.authenticate(authentication));
        assertEquals(lockedException.getMessage(),
                "Your account has been locked due to 3 failed login attempts. " +
                        "Please try again later.");

        verify(authenticationService, times(1)).loadUserByUsername(user.getEmail());
        verify(passwordEncoder, times(1))
                .matches(user.getPassword(), user.getPassword());
        verify(user, times(1)).isAccountNonLocked();
        verify(authenticationService, times(1)).isLockTimeExpired(user);
    }

    @Test
    @DisplayName("given authentication is UsernamePasswordAuthenticationToken object," +
            "when supports, then return true")
    void supports_validAuthentication() {
        boolean result = customAuthenticationProvider.supports(authentication.getClass());
        assertTrue(result);
    }

    @Test
    @DisplayName("given authentication is not UsernamePasswordAuthenticationToken object," +
            "when supports, then return false")
    void supports_invalidAuthentication() {
        TestingAuthenticationToken authentication
                = new TestingAuthenticationToken("user", "password");
        boolean result = customAuthenticationProvider.supports(authentication.getClass());
        assertFalse(result);
    }

}