package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.ResetPasswordDTO;
import com.example.demo.entities.ResetPasswordToken;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.OrderHistoryRepository;
import com.example.demo.repositories.ResetPasswordTokenRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class PasswordIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/password";

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
    private OrderHistoryRepository orderHistoryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ResetPasswordTokenRepository resetPasswordTokenRepository;

    public PasswordIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private ChangePasswordDTO changePasswordDTO = TestDataBuilder.buildChangePasswordDTO();
    private ResetPasswordDTO resetPasswordDTO = TestDataBuilder.buildResetPasswordDTO();
    private ResetPasswordToken resetPasswordToken = TestDataBuilder.buildResetPasswordTokenNoId(customer);

    private void insertCustomerHashedPassword(){
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        userRepository.save(customer);
    }

    private void insertResetPasswordToken(){
        resetPasswordTokenRepository.save(resetPasswordToken);
    }

    Customer setupCustomer() {
        return (Customer) userRepository.findByEmail(customer.getEmail())
                .orElseGet(() -> userRepository.save(customer));
    }

    @Test
    void givenValidBodyAndNoUser_whenChangePassword_thenChangeUserPasswordAndReturnNoContent() throws Exception {
        insertCustomerHashedPassword();

        mockMvc.perform(mockPostRequest("change-password", changePasswordDTO)
                        .with(user(setupCustomer())))
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findByEmail(customer.getEmail()).get();
        assertTrue(passwordEncoder.matches(changePasswordDTO.getNewPassword(), updatedUser.getPassword()));
    }

    @Test
    void givenInvalidBodyAndNoUser_whenChangePassword_thenThrowInvalidOldPasswordException() throws Exception {
        mockMvc.perform(mockPostRequest("change-password", changePasswordDTO)
                        .with(user(setupCustomer())))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidOldPasswordException));


    }@Test
    void givenValidBodyAndNoUser_whenResetPassword_thenResetPasswordAndReturnOk() throws Exception {
        customer.setResetPasswordToken(resetPasswordToken);
        insertCustomerHashedPassword();
        resetPasswordDTO.setPassword("new password");

        MockHttpServletRequestBuilder mockRequest = mockPostRequestWithParams
                ("reset-password", resetPasswordDTO,
                        "token", resetPasswordToken.getResetPasswordToken().toString());

        mockMvc.perform(mockRequest.with(user(setupCustomer()))).andExpect(status().isOk());

        User updatedUser = userRepository.findByEmail(customer.getEmail()).get();
        assertTrue(passwordEncoder.matches(resetPasswordDTO.getPassword(), updatedUser.getPassword()));
    }

}