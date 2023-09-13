package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.entities.exceptions.NoActiveOrderException;
import com.example.demo.services.exceptions.InsufficientBalanceException;
import com.example.demo.services.exceptions.InvalidOrderException;
import com.example.demo.services.exceptions.StripeErrorException;
import com.example.demo.services.stripe.StripeService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/payment";

    @MockBean
    private StripeService stripeService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderPaymentDTO orderPaymentDTO = TestDataBuilder.buildOrderPaymentDTO();
    private PaymentResponse mockPaymentResponse = mock(PaymentResponse.class);

    private MockHttpServletRequestBuilder mockPostRequest
            (Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidBody_whenCreateOrderPayment_thenReturnProductAndCreated() throws Exception {
        when(stripeService.createOrderPayment(orderPaymentDTO)).thenReturn(mockPaymentResponse);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockPaymentResponse)));

        verify(stripeService, times(1)).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenCreateOrderPayment_thenHandleMethodArgumentNotValidException() throws Exception {
        OrderPaymentDTO invalidOrderPaymentDTO = mock(OrderPaymentDTO.class);
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(invalidOrderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(stripeService, never()).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenStripeError_whenCreateOrderPayment_thenHandleStripeErrorException() throws Exception {
        when(stripeService.createOrderPayment(orderPaymentDTO)).thenThrow(StripeErrorException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof StripeErrorException));

        verify(stripeService, times(1)).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInsufficientBalance_whenCreateOrderPayment_thenHandleInsufficientBalanceException() throws Exception {
        when(stripeService.createOrderPayment(orderPaymentDTO)).thenThrow(InsufficientBalanceException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isPaymentRequired())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InsufficientBalanceException));

        verify(stripeService, times(1)).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderHasNoItems_whenCreateOrderPayment_thenHandleInvalidOrderException() throws Exception {
        when(stripeService.createOrderPayment(orderPaymentDTO)).thenThrow(InvalidOrderException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof InvalidOrderException));

        verify(stripeService, times(1)).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenUserHasNoActiveOrder_whenCreateOrderPayment_thenHandleInvalidOrderException() throws Exception {
        when(stripeService.createOrderPayment(orderPaymentDTO)).thenThrow(NoActiveOrderException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof NoActiveOrderException));

        verify(stripeService, times(1)).createOrderPayment(orderPaymentDTO);
    }

    @Test
    void givenNoUser_whenCreateOrderPayment_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(stripeService, never()).createOrderPayment(orderPaymentDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenCreateOrderPayment_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderPaymentDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(stripeService, never()).createOrderPayment(orderPaymentDTO);
    }
}