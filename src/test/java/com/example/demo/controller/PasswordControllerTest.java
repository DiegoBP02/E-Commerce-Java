package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.services.PasswordService;
import com.example.demo.services.exceptions.InvalidOldPasswordException;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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

    private MockHttpServletRequestBuilder mockPostRequest
            (String endpoint, Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    @Test
    @WithMockUser
    void givenValidBody_whenChangePassword_thenReturnNoContent() throws Exception {
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password",changePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isNoContent());

        verify(passwordService, times(1)).changePassword(changePasswordDTO);
    }

    @Test
    @WithMockUser
    void givenInvalidBody_whenChangePassword_thenHandleMethodArgumentNotValidException() throws Exception {
        ChangePasswordDTO invalidChangePasswordDTO = new ChangePasswordDTO();
        MockHttpServletRequestBuilder mockRequest
                = mockPostRequest("change-password",invalidChangePasswordDTO);

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
                = mockPostRequest("change-password",changePasswordDTO);

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
                = mockPostRequest("change-password",changePasswordDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(passwordService, never()).changePassword(changePasswordDTO);
    }

}