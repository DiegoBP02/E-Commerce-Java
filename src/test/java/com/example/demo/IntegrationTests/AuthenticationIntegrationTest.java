package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.AuthenticationService;
import com.example.demo.services.exceptions.InvalidRoleException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class AuthenticationIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/auth";

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthenticationIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private Seller seller = (Seller) TestDataBuilder.buildUserNoId();
    private RegisterDTO registerDTO = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDTO = TestDataBuilder.buildLoginDTO();

    private void insertSellerValidPassword() {
        seller.setPassword(passwordEncoder.encode(seller.getPassword()));
        userRepository.save(seller);
    }

    private void insertSellerWrongPassword() {
        userRepository.save(seller);
    }

    // AuthenticationController tests

    @Test
    void givenValidBodySellerAndNoUser_whenRegister_thenReturnUserLoginResponseDTOAndCreateUser() throws Exception {
        registerDTO.setRole(Role.Seller);

        mockMvc.perform(mockPostRequest("register", registerDTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value(registerDTO.getRole().toString()));

        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    void givenValidBodyCustomerAndNoUser_whenRegister_thenReturnUserLoginResponseDTOAndCreateUser() throws Exception {
        registerDTO.setRole(Role.Customer);

        mockMvc.perform(mockPostRequest("register", registerDTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value(registerDTO.getRole().toString()));

        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    void givenValidBodyAdminAndNoUser_whenRegister_thenThrowInvalidRoleException() throws Exception {
        registerDTO.setRole(Role.Admin);

        mockMvc.perform(mockPostRequest("register", registerDTO))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidRoleException));

        assertEquals(0, userRepository.findAll().size());
    }

    @Test
    void givenValidBodyAndNoUser_whenLogin_thenReturnUserLoginResponseDto() throws Exception {
        insertSellerValidPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(loginDTO.getEmail()));
    }

    // CustomAuthenticationProvider tests

    @Test
    @DisplayName("given user and valid credentials and failed attempts, when authenticate, " +
            "then reset user failed attempts and return UsernamePasswordAuthenticationToken")
    void authenticate_validUserFailedAttempts() throws Exception {
        seller.setFailedAttempt(1);
        insertSellerValidPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(loginDTO.getEmail()));

        Seller updatedSeller = (Seller) userRepository.findByEmail(seller.getEmail()).get();
        assertEquals(0, updatedSeller.getFailedAttempt());
    }

    @Test
    @DisplayName("given user is non locked and failed attempts are smaller than " +
            "MAX_FAILED_ATTEMPTS, when authenticate, then increase user failed attempts and" +
            "throw BadCredentialsException")
    void authenticate_invalidUser() throws Exception {
        insertSellerWrongPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof BadCredentialsException));

        Seller updatedSeller = (Seller) userRepository.findByEmail(seller.getEmail()).get();
        assertEquals(1, updatedSeller.getFailedAttempt());
    }

    @Test
    @DisplayName("given user is non locked and failed attempts reach MAX_FAILED_ATTEMPTS," +
            "when authenticate, then lock user account and throw LockedException")
    void authenticate_invalidUserMaxFailedAttempts() throws Exception {
        seller.setFailedAttempt(2);
        insertSellerWrongPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isLocked())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof LockedException));

        Seller updatedSeller = (Seller) userRepository.findByEmail(seller.getEmail()).get();
        assertEquals(2, updatedSeller.getFailedAttempt());
        assertFalse(updatedSeller.isAccountNonLocked());
        assertNotNull(updatedSeller.getLockTime());
    }

    @Test
    @DisplayName("given user is locked and lock time expired, when authenticate," +
            "then throw LockedException warning user account has been unlocked")
    void authenticate_userLockedLockTimeExpired() throws Exception {
        seller.setLockTime(Instant.now().minusSeconds(60 * 60 * 24));
        seller.setAccountNonLocked(false);
        seller.setFailedAttempt(1);
        insertSellerValidPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isLocked())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof LockedException));

        Seller updatedSeller = (Seller) userRepository.findByEmail(seller.getEmail()).get();
        assertTrue(updatedSeller.isAccountNonLocked());
        assertNull(updatedSeller.getLockTime());
        assertEquals(0, updatedSeller.getFailedAttempt());
    }

    @Test
    @DisplayName("given user is locked and lock time has not expired, when authenticate," +
            "then throw LockedException warning that the account has been locked")
    void authenticate_userLocked() throws Exception {
        seller.setLockTime(Instant.now());
        seller.setAccountNonLocked(false);
        seller.setFailedAttempt(3);
        insertSellerValidPassword();

        mockMvc.perform(mockPostRequest("login", loginDTO))
                .andExpect(status().isLocked())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof LockedException));

        assertFalse(seller.isAccountNonLocked());
        assertNotNull(seller.getLockTime());
        assertEquals(3, seller.getFailedAttempt());
    }

}