package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.enums.Role;
import com.example.demo.exceptions.UniqueConstraintViolationError;
import com.example.demo.services.AuthenticationService;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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

    private RegisterDTO registerDto = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDto = TestDataBuilder.buildLoginDTO();
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
        when(authenticationService.register(any(RegisterDTO.class))).thenReturn(token);
        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/register", registerDto);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string(token));

        verify(authenticationService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    void givenInvalidBody_whenRegister_thenHandleMethodArgumentNotValidException() throws Exception {
        RegisterDTO registerDTO = new RegisterDTO();

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/register", registerDTO);

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
                ("/register", registerDto);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UniqueConstraintViolationError));

        verify(authenticationService, times(1)).register(any(RegisterDTO.class));
    }

    @Test
    void givenUser_whenLogin_thenReturnToken() throws Exception {
        when(authenticationService.login(any(LoginDTO.class))).thenReturn("token");


        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDto);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("token"));

        verify(authenticationService, times(1)).login(any(LoginDTO.class));
    }

    @Test
    void givenInvalidBody_whenLogin_thenHandleMethodArgumentNotValidException() throws Exception {
        LoginDTO loginDTO = new LoginDTO();

        MockHttpServletRequestBuilder mockRequest = buildMockRequestPost
                ("/login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(authenticationService, never()).login(any(LoginDTO.class));
    }

}