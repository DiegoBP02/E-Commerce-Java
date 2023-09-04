package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.ForgotPasswordDTO;
import com.example.demo.dtos.ResetPasswordDTO;
import com.example.demo.services.PasswordService;
import com.example.demo.services.exceptions.EmailSendException;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.services.exceptions.InvalidTokenException;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/password";

    @MockBean
    private PasswordService passwordService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ChangePasswordDTO changePasswordDTO = TestDataBuilder.buildChangePasswordDTO();
    private ForgotPasswordDTO forgotPasswordDTO = TestDataBuilder.buildForgotPasswordDTO();
    private ResetPasswordDTO resetPasswordDTO = TestDataBuilder.buildResetPasswordDTO();
    private UUID randomToken = UUID.randomUUID();

    private MockHttpServletRequestBuilder mockPostRequest
            (String endpoint, Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    private MockHttpServletRequestBuilder mockPostRequestWithParams
            (String endpoint, Object requestObject, String paramName, String paramValue)
            throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .param(paramName, paramValue)
                .content(this.objectMapper.writeValueAsString(requestObject))
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @WithMockUser
    void givenValidBody_whenChangePassword_thenReturnNoContent() throws Exception {
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password", changePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isNoContent());

        verify(passwordService, times(1)).changePassword(changePasswordDTO);
    }

    @Test
    @WithMockUser
    void givenInvalidBody_whenChangePassword_thenHandleMethodArgumentNotValidException() throws Exception {
        ChangePasswordDTO invalidChangePasswordDTO = new ChangePasswordDTO();
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password", invalidChangePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(passwordService, never()).changePassword(changePasswordDTO);
    }

    @Test
    @WithMockUser
    void givenInvalidOldPassword_whenChangePassword_thenHandleInvalidOldPasswordException() throws Exception {
        doThrow(InvalidOldPasswordException.class)
                .when(passwordService).changePassword(changePasswordDTO);
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password", changePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidOldPasswordException));

        verify(passwordService, times(1)).changePassword(changePasswordDTO);
    }

    @Test
    void givenNoUser_whenChangePassword_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password", changePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(passwordService, never()).changePassword(changePasswordDTO);
    }

    @Test
    void givenValidForgotPasswordDTO_whenForgotPassword_thenReturnOkWithCorrectMessage() throws Exception {
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("forgot-password", forgotPasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("We have sent a reset " +
                        "password link to your email. Please check."));

        verify(passwordService, times(1))
                .forgotPassword(any(HttpServletRequest.class), eq(forgotPasswordDTO));
    }

    @Test
    void givenInvalidBody_whenForgotPassword_thenHandleMethodArgumentNotValidException() throws Exception {
        ForgotPasswordDTO invalidForgotPassword = new ForgotPasswordDTO();
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("forgot-password", invalidForgotPassword);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(passwordService, never())
                .forgotPassword(any(HttpServletRequest.class), eq(invalidForgotPassword));
    }

    @Test
    void givenErrorWhileSendingEmail_whenForgotPassword_thenHandleEmailSendException() throws Exception {
        doThrow(EmailSendException.class)
                .when(passwordService)
                .forgotPassword(any(HttpServletRequest.class), eq(forgotPasswordDTO));
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("forgot-password", forgotPasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isInternalServerError())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof EmailSendException));

        verify(passwordService, times(1))
                .forgotPassword(any(HttpServletRequest.class), eq(forgotPasswordDTO));
    }

    @Test
    void givenValidResetPasswordDTO_whenResetPassword_thenReturnOkWithCorrectMessage() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams("reset-password", resetPasswordDTO,
                        "token", randomToken.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("You have successfully changed your password."));

        verify(passwordService, times(1))
                .resetPassword(randomToken, resetPasswordDTO);
    }

    @Test
    void givenInvalidBody_whenResetPassword_thenHandleMethodArgumentNotValidException() throws Exception {
        ResetPasswordDTO invalidResetPasswordDTO = new ResetPasswordDTO();
        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams("reset-password", invalidResetPasswordDTO,
                        "token", randomToken.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(passwordService, never()).resetPassword(randomToken, invalidResetPasswordDTO);
    }

    @Test
    void givenInvalidToken_whenResetPassword_thenHandleInvalidTokenException() throws Exception {
        doThrow(InvalidTokenException.class)
                .when(passwordService)
                .resetPassword(randomToken, resetPasswordDTO);

        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams("reset-password", resetPasswordDTO,
                        "token", randomToken.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidTokenException));

        verify(passwordService, times(1))
                .resetPassword(randomToken, resetPasswordDTO);
    }

    @Test
    void givenMissingParam_whenResetPassword_thenHandleMissingServletRequestParameterException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .param("token", "")
                .content(this.objectMapper.writeValueAsString(resetPasswordDTO))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MissingServletRequestParameterException));

        verify(passwordService, never()).resetPassword(randomToken, resetPasswordDTO);
    }

    @Test
    void givenWrongParan_whenResetPassword_thenHandleMethodArgumentTypeMismatchException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = MockMvcRequestBuilders
                .post(PATH + "/reset-password")
                .param("token", "random")
                .content(this.objectMapper.writeValueAsString(resetPasswordDTO))
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentTypeMismatchException));

        verify(passwordService, never()).resetPassword(randomToken, resetPasswordDTO);
    }

}