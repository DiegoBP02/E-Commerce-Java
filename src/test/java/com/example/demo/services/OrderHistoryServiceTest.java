package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.repositories.OrderHistoryRepository;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class OrderHistoryServiceTest extends ApplicationConfigTest {

    @Autowired
    private OrderHistoryService orderHistoryService;

    @MockBean
    private OrderHistoryRepository orderHistoryRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Customer customer = TestDataBuilder.buildCustomer();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistory(order);
    private OrderHistoryDTO orderHistoryDTO = TestDataBuilder.buildOrderHistoryDTO(order);

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenOrderHistoryDTO_whenCreate_thenReturnOrderHistory(){
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenReturn(orderHistory);

        OrderHistory result = orderHistoryService.create(orderHistoryDTO);

        assertEquals(orderHistory,result);

        verify(orderHistoryRepository,times(1)).save(any(OrderHistory.class));
        verifyAuthentication();
    }

}