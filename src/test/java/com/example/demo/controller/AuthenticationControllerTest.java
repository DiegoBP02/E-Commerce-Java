package com.example.demo.controller;

import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.dtos.UserLoginResponseDTO;
import com.example.demo.services.AuthenticationService;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationControllerTest extends ApplicationConfigTestController {

    private static final String PATH = "/auth";

    public AuthenticationControllerTest() {
        super(PATH);
    }

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterDTO registerDTO = TestDataBuilder.buildRegisterDTO();
    private LoginDTO loginDTO = TestDataBuilder.buildLoginDTO();
    private UUID randomUUID = UUID.randomUUID();
    private UserLoginResponseDTO userLoginResponseDTO = TestDataBuilder.buildUserLoginResponseDTO(registerDTO);

    @Test
    void givenValidUser_whenRegister_thenReturnTokenAndOk() throws Exception {
        when(authenticationService.register(registerDTO)).thenReturn(userLoginResponseDTO);
        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("register", registerDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(userLoginResponseDTO)));

        verify(authenticationService, times(1)).register(registerDTO);
    }

    @Test
    void givenRoleIsAdmin_whenRegister_thenHandleInvalidRoleException() throws Exception {
        when(authenticationService.register(registerDTO)).thenThrow(InvalidRoleException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("register", registerDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidRoleException));

        verify(authenticationService, times(1)).register(registerDTO);
    }

    @Test
    void givenInvalidBody_whenRegister_thenHandleMethodArgumentNotValidException() throws Exception {
        RegisterDTO invalidRegisterDTO = new RegisterDTO();

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("register", invalidRegisterDTO);

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

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("register", registerDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UniqueConstraintViolationError));

        verify(authenticationService, times(1)).register(registerDTO);
    }

    @Test
    void givenUser_whenLogin_thenReturnToken() throws Exception {
        when(authenticationService.login(loginDTO)).thenReturn(userLoginResponseDTO);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(userLoginResponseDTO)));

        verify(authenticationService, times(1)).login(loginDTO);
    }

    @Test
    void givenInvalidBody_whenLogin_thenHandleMethodArgumentNotValidException() throws Exception {
        LoginDTO invalidLoginDTO = new LoginDTO();

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("login", invalidLoginDTO);

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

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("login", loginDTO);

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

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("login", loginDTO);

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

        MockHttpServletRequestBuilder mockRequest = mockPostRequest
                ("login", loginDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isLocked())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof LockedException));

        verify(authenticationService, times(1)).login(loginDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenRequestAndCustomer_whenConfirmationRequest_thenReturnOkWithMessage() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("We have sent a confirmation " +
                        "account link to your email. Please check."));

        verify(authenticationService, times(1))
                .confirmationRequest(any(HttpServletRequest.class));
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenRequestAndSeller_whenConfirmationRequest_thenReturnOkWithMessage() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("We have sent a confirmation " +
                        "account link to your email. Please check."));

        verify(authenticationService, times(1))
                .confirmationRequest(any(HttpServletRequest.class));
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenUserAlreadyEnabled_whenConfirmationRequest_thenHandleUserAlreadyEnabledException() throws Exception {
        doThrow(UserAlreadyEnabledException.class).when(authenticationService)
                .confirmationRequest(any(HttpServletRequest.class));

        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UserAlreadyEnabledException));

        verify(authenticationService, times(1))
                .confirmationRequest(any(HttpServletRequest.class));
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenConfirmationTokenAlreadyExists_whenConfirmationRequest_thenHandleConfirmationTokenAlreadyExistsException() throws Exception {
        doThrow(ConfirmationTokenAlreadyExistsException.class).when(authenticationService)
                .confirmationRequest(any(HttpServletRequest.class));

        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ConfirmationTokenAlreadyExistsException));

        verify(authenticationService, times(1))
                .confirmationRequest(any(HttpServletRequest.class));
    }

    @Test
    void givenNoUser_whenConfirmationRequest_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verifyNoInteractions(authenticationService);
    }

    @Test
    @WithMockUser(authorities = "random")
    void givenInvalidUserAuthority_whenConfirmationRequest_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockGetRequest("confirmation-request");

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenRequestAndToken_whenConfirmAccount_thenReturnOkWithMessage() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams
                        ("confirm-account", "token", randomUUID.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("You have successfully confirmed your account."));

        verify(authenticationService, times(1))
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));
    }

    @Test
    void givenConfirmationTokenNotFound_whenConfirmAccount_thenHandleEntityNotFoundException() throws Exception {
        doThrow(EntityNotFoundException.class).when(authenticationService)
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));

        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams
                        ("confirm-account", "token", randomUUID.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof EntityNotFoundException));

        verify(authenticationService, times(1))
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenUserNotFound_whenConfirmAccount_thenHandleUserNotFoundException() throws Exception {
        doThrow(UserNotFoundException.class).when(authenticationService)
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));

        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams
                        ("confirm-account", "token", randomUUID.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UserNotFoundException));

        verify(authenticationService, times(1))
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenConfirmationTokenExpired_whenConfirmAccount_thenHandleConfirmationTokenExpiredException() throws Exception {
        doThrow(ConfirmationTokenExpiredException.class).when(authenticationService)
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));

        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams
                        ("confirm-account", "token", randomUUID.toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ConfirmationTokenExpiredException));

        verify(authenticationService, times(1))
                .confirmAccount(any(HttpServletRequest.class), eq(randomUUID));
    }

    @Test
    void givenMissingParam_whenConfirmAccount_thenHandleMissingServletRequestParameterException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest("confirm-account");

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MissingServletRequestParameterException));

        verifyNoInteractions(authenticationService);
    }

    @Test
    void givenInvalidParam_whenConfirmAccount_thenHandleMethodArgumentTypeMismatchException() throws Exception {
        MockHttpServletRequestBuilder mockRequest =
                mockPostRequestWithParams
                        ("confirm-account", "token", "random");

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentTypeMismatchException));

        verifyNoInteractions(authenticationService);
    }

}