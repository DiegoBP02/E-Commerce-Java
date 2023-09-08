package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.ConfirmationToken;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ConfirmationTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
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
    @MockBean
    private ConfirmationTokenRepository confirmationTokenRepository;
    @MockBean
    private EmailService emailService;

    private Authentication authentication;
    private SecurityContext securityContext;
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private String URL = "URL";

    private User user = TestDataBuilder.buildUser();
    private RegisterDTO registerDTO = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDTO = TestDataBuilder.buildLoginDTO();
    private ConfirmationToken confirmationToken = new ConfirmationToken(user);
    private ConfirmationToken mockConfirmationToken = mock(ConfirmationToken.class);
    private String token = "token";
    private UUID randomUUID = UUID.randomUUID();

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockRequest() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getServletPath()).thenReturn("/path");
    }

    private void verifyMockRequest() {
        verify(request,times(1)).getRequestURL();
        verify(request,times(1)).getServletPath();
    }

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
        assertThat(user.getLockTime()).isCloseTo(Instant.now(), temporalUnitOffset);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void givenUserAndLockTimeIsExpired_whenIsLockTimeExpired_thenReturnTrueAndUnlockUser(){
        user.setLockTime(Instant.now().minusSeconds(60 * 60 * 24 * 50));

        boolean result = authenticationService.isLockTimeExpired(user);
        assertTrue(result);
        assertTrue(user.isAccountNonLocked());
        assertNull(user.getLockTime());
        assertEquals(0, user.getFailedAttempt());

        verify(userRepository,times(1)).save(user);
    }

    @Test
    void givenUserAndLockTimeIsNotExpired_whenIsLockTimeExpired_thenReturnFalseAndDoesNotUnlockUser(){
        user.setLockTime(Instant.now());
        user.setAccountNonLocked(false);

        boolean result = authenticationService.isLockTimeExpired(user);
        assertFalse(result);
        assertFalse(user.isAccountNonLocked());

        verify(userRepository,never()).save(user);
    }

    @Test
    void givenUserIsAlreadyEnabled_whenConfirmationRequest_thenThrowUserAlreadyEnabledException(){
        user.setEnabled(true);

        assertThrows(UserAlreadyEnabledException.class,
                () -> authenticationService.confirmationRequest(request));

        verifyAuthentication();
        verifyNoInteractions(confirmationTokenRepository,emailService);
    }

    @Test
    void givenConfirmationTokenAlreadyExistsAndIsExpired_whenConfirmationRequest_thenResetTokenAndSendEmailWithTokenConfirmation(){
        mockRequest();
        when(confirmationTokenRepository.findByUser(user)).thenReturn(mockConfirmationToken);
        when(mockConfirmationToken.isTokenExpired()).thenReturn(true);

        authenticationService.confirmationRequest(request);

        verifyAuthentication();
        verifyMockRequest();
        verify(confirmationTokenRepository,times(1)).findByUser(user);
        verify(mockConfirmationToken,times(1)).isTokenExpired();
        verify(mockConfirmationToken,times(1)).resetToken();
        verify(confirmationTokenRepository,times(1)).save(mockConfirmationToken);
        verify(emailService,times(1))
                .sendEmail(eq(user.getEmail()), anyString(),anyString());
    }

    @Test
    void givenConfirmationTokenAlreadyExistsAndIsNotExpired_whenConfirmationRequest_thenThrowConfirmationTokenAlreadyExistsExceptionAndSendEmailWithTokenConfirmation(){
        when(confirmationTokenRepository.findByUser(user)).thenReturn(mockConfirmationToken);
        when(mockConfirmationToken.isTokenExpired()).thenReturn(false);

        assertThrows(ConfirmationTokenAlreadyExistsException.class,
                () -> authenticationService.confirmationRequest(request));

        verifyAuthentication();
        verify(confirmationTokenRepository,times(1)).findByUser(user);
        verify(mockConfirmationToken,times(1)).isTokenExpired();
        verify(mockConfirmationToken,times(1)).getTimeUntilExpiration();
        verifyNoMoreInteractions(confirmationTokenRepository, emailService,mockConfirmationToken);
    }

    @Test
    void givenConfirmationDoesNotExists_whenConfirmationRequest_thenCreateNewConfirmationTokenAndSendEmailWithTokenConfirmation(){
        mockRequest();
        when(confirmationTokenRepository.findByUser(user)).thenReturn(null);

        authenticationService.confirmationRequest(request);

        verifyAuthentication();
        verifyMockRequest();
        verify(confirmationTokenRepository,times(1)).findByUser(user);
        verify(confirmationTokenRepository,times(1))
                .save(any(ConfirmationToken.class));
        verify(emailService,times(1))
                .sendEmail(eq(user.getEmail()), anyString(),anyString());
    }

    @Test
    void givenRequestAndTokenIsNotExpired_whenConfirmAccount_thenEnableAccountAndDeleteConfirmationToken(){
        when(confirmationTokenRepository.findByConfirmationToken(randomUUID))
                .thenReturn(Optional.of(confirmationToken));
        when(userRepository.findByEmail(confirmationToken.getUser().getEmail()))
                .thenReturn(Optional.of(user));

        authenticationService.confirmAccount(request, randomUUID);
        assertTrue(user.isEnabled());

        verify(confirmationTokenRepository,times(1))
                .findByConfirmationToken(randomUUID);
        verify(userRepository,times(1))
                .findByEmail(confirmationToken.getUser().getEmail());
        verify(userRepository,times(1)).save(user);
        verify(confirmationTokenRepository,times(1))
                .deleteById(confirmationToken.getId());
        verifyNoInteractions(emailService);
    }

    @Test
    void givenNoConfirmation_whenConfirmAccount_thenThrowEntityNotFoundException(){
        when(confirmationTokenRepository.findByConfirmationToken(randomUUID))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> authenticationService.confirmAccount(request,randomUUID));

        verify(confirmationTokenRepository,times(1))
                .findByConfirmationToken(randomUUID);
        verifyNoMoreInteractions(confirmationTokenRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void givenNoUser_whenConfirmAccount_thenThrowUserNotFoundException(){
        when(confirmationTokenRepository.findByConfirmationToken(randomUUID))
                .thenReturn(Optional.of(confirmationToken));
        when(userRepository.findByEmail(confirmationToken.getUser().getEmail()))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> authenticationService.confirmAccount(request,randomUUID));

        verify(confirmationTokenRepository,times(1))
                .findByConfirmationToken(randomUUID);
        verify(userRepository,times(1))
                .findByEmail(confirmationToken.getUser().getEmail());
        verifyNoMoreInteractions(confirmationTokenRepository,userRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    void givenConfirmationTokenIsExpired_whenConfirmAccount_thenResetTokenAndThrowConfirmationTokenException(){
        mockRequest();
        when(mockConfirmationToken.getUser()).thenReturn(user);
        when(mockConfirmationToken.getConfirmationToken()).thenReturn(randomUUID);
        when(confirmationTokenRepository.findByConfirmationToken(randomUUID))
                .thenReturn(Optional.of(mockConfirmationToken));
        when(userRepository.findByEmail(confirmationToken.getUser().getEmail()))
                .thenReturn(Optional.of(user));
        when(mockConfirmationToken.isTokenExpired()).thenReturn(true);

        assertThrows(ConfirmationTokenExpiredException.class,
                () -> authenticationService.confirmAccount(request, randomUUID));

        verifyMockRequest();
        verify(confirmationTokenRepository,times(1))
                .findByConfirmationToken(randomUUID);
        verify(userRepository,times(1))
                .findByEmail(confirmationToken.getUser().getEmail());
        verify(mockConfirmationToken, times(1)).isTokenExpired();
        verify(mockConfirmationToken, times(1)).resetToken();
        verify(emailService,times(1))
                .sendEmail(eq(user.getEmail()), anyString(),anyString());
        verifyNoMoreInteractions(confirmationTokenRepository,userRepository,emailService);
    }

}