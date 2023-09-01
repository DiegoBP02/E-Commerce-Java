package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.services.exceptions.UniqueConstraintViolationError;
import com.example.demo.services.AuthenticationService;
import com.example.demo.services.exceptions.UserNotFoundException;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationControllerTest extends ApplicationConfigTest {
    private static final String PATH = "/auth";

    @MockBean
    private AuthenticationService authenticationService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private RegisterDTO registerDTO = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDTO = TestDataBuilder.buildLoginDTO();
    private String token = "token";

    private MockHttpServletRequestBuilder buildMockRequestPost
            (String endpoint, Object requestObject) throws Exception {
        return MockMvcRequestBuilders
                .post(PATH + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    @Test
    void givenValidUser_whenRegister_thenReturnTokenAndOk() throws Exception {
        when(authenticationService.register(registerDTO)).thenReturn(token);
        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/register", registerDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(token));

        verify(authenticationService, times(1)).register(registerDTO);
    }

    @Test
    void givenInvalidBody_whenRegister_thenHandleMethodArgumentNotValidException() throws Exception {
        RegisterDTO invalidRegisterDTO = new RegisterDTO();

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/register", invalidRegisterDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(authenticationService, never()).register(any(RegisterDTO.class));
    }

    @Test
    void givenUserAlreadyExists_whenRegister_thenHandleUniqueConstraintViolationError()
            throws Exception {
        when(authenticationService.register(any(RegisterDTO.class)))
                .thenThrow(UniqueConstraintViolationError.class);

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/register", registerDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UniqueConstraintViolationError));

        verify(authenticationService, times(1)).register(registerDTO);
    }

    @Test
    void givenUser_whenLogin_thenReturnToken() throws Exception {
        when(authenticationService.login(loginDTO)).thenReturn("token");


        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("token"));

        verify(authenticationService, times(1)).login(loginDTO);
    }

    @Test
    void givenInvalidBody_whenLogin_thenHandleMethodArgumentNotValidException() throws Exception {
        LoginDTO invalidLoginDTO = new LoginDTO();

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", invalidLoginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(authenticationService, never()).login(any(LoginDTO.class));
    }

    @Test
    void givenUserDoesNotExists_whenLogin_thenHandleUserNotFoundException() throws Exception {
        when(authenticationService.login(loginDTO)).thenThrow(UserNotFoundException.class);

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UserNotFoundException));

        verify(authenticationService, times(1)).login(loginDTO);
    }

    @Test
    void givenInvalidCredentials_whenLogin_thenHandleBadCredentialsException() throws Exception {
        when(authenticationService.login(loginDTO)).thenThrow(BadCredentialsException.class);

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof BadCredentialsException));

        verify(authenticationService, times(1)).login(loginDTO);
    }

    @Test
    void givenUserIsLocked_whenLogin_thenHandleLockedException() throws Exception {
        when(authenticationService.login(loginDTO)).thenThrow(LockedException.class);

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isLocked())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof LockedException));

        verify(authenticationService, times(1)).login(loginDTO);
    }

}