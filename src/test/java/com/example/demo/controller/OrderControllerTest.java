package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.Order;
import com.example.demo.services.OrderService;
import com.example.demo.services.exceptions.ActiveOrderAlreadyExistsException;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/orders";

    @MockBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Order order = TestDataBuilder.buildOrderWithId();
    private List<Order> orders = TestDataBuilder.buildList(order);

    private MockHttpServletRequestBuilder mockPostRequest() throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder mockGetRequest() {
        return MockMvcRequestBuilders
                .get(PATH)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder mockGetRequest(String endpoint) {
        return MockMvcRequestBuilders
                .get(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder mockDeleteRequest(String endpoint) {
        return MockMvcRequestBuilders
                .delete(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidBody_whenCreate_thenReturnOrderAndCreated() throws Exception {
        when(orderService.create()).thenReturn(order);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest();

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(order)));

        verify(orderService, times(1)).create();
    }

    @Test
    void givenNoUser_whenCreate_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest();

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).create();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoExistingActiveOrder_whenCreate_thenHandleActiveOrderAlreadyExistsException() throws Exception {
        when(orderService.create()).thenThrow(ActiveOrderAlreadyExistsException.class);
        MockHttpServletRequestBuilder mockRequest = mockPostRequest();

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ActiveOrderAlreadyExistsException));

        verify(orderService, times(1)).create();
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenCreate_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest();

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).create();
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void givenOrdersAndUserIsAdmin_whenFindAll_thenReturnOrders() throws Exception {
        when(orderService.findAll()).thenReturn(orders);

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orders)));

        verify(orderService, times(1)).findAll();
    }

    @Test
    void givenNoUser_whenFindAll_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthoritySeller_whenFindAll_thenHandleAccessDeniedException() throws Exception {

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthorityCustomer_whenFindAll_thenHandleAccessDeniedException() throws Exception {

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderAndValidUser_whenFindById_thenReturnOrder() throws Exception {
        when(orderService.findById(order.getId())).thenReturn(order);

        mockMvc.perform(mockGetRequest(order.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(order)));

        verify(orderService, times(1)).findById(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoOrderAndNoUser_whenFindById_thenHandleResourceNotFoundException() throws Exception {
        when(orderService.findById(order.getId()))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest(order.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderService, times(1)).findById(order.getId());
    }

    @Test
    void givenNoUser_whenFindById_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest(order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).findById(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindById_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest(order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).findById(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderAndValidUser_whenFindActiveOrderByCurrentUser_thenReturnOrder() throws Exception {
        when(orderService.findActiveOrderByCurrentUser()).thenReturn(order);

        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(order)));

        verify(orderService, times(1)).findActiveOrderByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoOrderAndNoUser_whenFindActiveOrderByCurrentUser_thenHandleResourceNotFoundException() throws Exception {
        when(orderService.findActiveOrderByCurrentUser()).thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderService, times(1)).findActiveOrderByCurrentUser();
    }

    @Test
    void givenNoUser_whenFindActiveOrderByCurrentUser_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).findActiveOrderByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindActiveOrderByCurrentUser_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).findActiveOrderByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidUserAndOrder_whenDelete_thenReturnNoContent() throws Exception {
        mockMvc.perform(mockDeleteRequest(order.getId().toString()))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).delete(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderDoesNotBelongToUser_whenDelete_thenHandleUnauthorizedAccessException() throws Exception {
        doThrow(UnauthorizedAccessException.class)
                .when(orderService).delete(order.getId());

        mockMvc.perform(mockDeleteRequest(order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(orderService, times(1)).delete(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenDatabaseError_whenDelete_thenHandleDatabaseException() throws Exception {
        doThrow(DatabaseException.class)
                .when(orderService).delete(order.getId());

        mockMvc.perform(mockDeleteRequest(order.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof DatabaseException));

        verify(orderService, times(1)).delete(order.getId());
    }

    @Test
    void givenNoUser_whenDelete_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockDeleteRequest(order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).delete(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenDelete_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest(order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).delete(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidUserAndOrder_whenDeleteByCurrentUser_thenReturnNoContent() throws Exception {
        mockMvc.perform(mockDeleteRequest("user"))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderDoesNotBelongToUser_whenDeleteByCurrentUser_thenHandleUnauthorizedAccessException() throws Exception {
        doThrow(UnauthorizedAccessException.class)
                .when(orderService).deleteByCurrentUser();

        mockMvc.perform(mockDeleteRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(orderService, times(1)).deleteByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenDatabaseError_whenDeleteByCurrentUser_thenHandleDatabaseException() throws Exception {
        doThrow(DatabaseException.class)
                .when(orderService).deleteByCurrentUser();

        mockMvc.perform(mockDeleteRequest("user"))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof DatabaseException));

        verify(orderService, times(1)).deleteByCurrentUser();
    }

    @Test
    void givenNoUser_whenDeleteByCurrentUser_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockDeleteRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderService, never()).deleteByCurrentUser();
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenDeleteByCurrentUser_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderService, never()).deleteByCurrentUser();
    }

}