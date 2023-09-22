package com.example.demo.controller;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.services.OrderHistoryService;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderHistoryControllerTest extends ApplicationConfigTestController {

    private static final String PATH = "/orderHistory";

    public OrderHistoryControllerTest() {
        super(PATH);
    }

    @MockBean
    private OrderHistoryService orderHistoryService;

    private Customer customer = TestDataBuilder.buildCustomerWithId();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistoryWithId(order);

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderHistory_whenFindById_thenReturnOrderHistory() throws Exception {
        when(orderHistoryService.findById(orderHistory.getId())).thenReturn(orderHistory);

        mockMvc.perform(mockGetRequest(orderHistory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderHistory)));

        verify(orderHistoryService, times(1)).findById(orderHistory.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoOrderHistory_whenFindById_thenHandleResourceNotFoundException() throws Exception {
        when(orderHistoryService.findById(orderHistory.getId()))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest(orderHistory.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderHistoryService, times(1)).findById(orderHistory.getId());
    }

    @Test
    void givenNoUser_whenFindById_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest(orderHistory.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verifyNoInteractions(orderHistoryService);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindById_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest(orderHistory.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(orderHistoryService);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderHistory_whenFindByCurrentUser_thenReturnOrderHistoryPage() throws Exception {
        Page<OrderHistory> orderHistoryPage = mock(PageImpl.class);

        when(orderHistoryService.findByCurrentUser(0, 5, Sort.Direction.ASC, "paymentDate"))
                .thenReturn(orderHistoryPage);

        mockMvc.perform(mockGetRequest("/user"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderHistoryPage)));

        verify(orderHistoryService, times(1))
                .findByCurrentUser(0, 5, Sort.Direction.ASC, "paymentDate");
    }

    @Test
    void givenNoUser_whenFindByCurrentUser_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verifyNoInteractions(orderHistoryService);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindByCurrentUser_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(orderHistoryService);
    }

}