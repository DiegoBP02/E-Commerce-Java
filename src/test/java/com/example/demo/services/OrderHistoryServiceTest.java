package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.repositories.OrderHistoryRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

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
    private Page<OrderHistory> orderHistoryPage = new PageImpl<>
            (Collections.singletonList(orderHistory),
                    PageRequest.of(0, 5, Sort.by("paymentDate")), 1);

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

    @Test
    void givenOrderHistory_whenFindById_thenReturnOrderHistory() {
        when(orderHistoryRepository.findById(orderHistory.getId()))
                .thenReturn(Optional.of(orderHistory));

        OrderHistory result = orderHistoryService.findById(orderHistory.getId());

        assertEquals(orderHistory, result);

        verifyAuthentication();
        verify(orderHistoryRepository, times(1)).findById(order.getId());
    }

    @Test
    void givenNoOrder_whenFindById_thenThrowResourceNotFoundException() {
        when(orderHistoryRepository.findById(orderHistory.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderHistoryService.findById(order.getId()));

        verifyNoInteractions(authentication,securityContext);
        verify(orderHistoryRepository, times(1)).findById(order.getId());
    }

    @Test
    void givenUserIsNotTheOwnerOfTheOrderHistory_whenFindById_thenThrowUnauthorizedAccessException() {
        Customer customerMock = mock(Customer.class);
        when(customerMock.getId()).thenReturn(UUID.randomUUID());
        OrderHistory orderHistoryMock = mock(OrderHistory.class);
        when(orderHistoryMock.getCustomer()).thenReturn(customerMock);
        when(orderHistoryRepository.findById(orderHistory.getId()))
                .thenReturn(Optional.of(orderHistoryMock));

        assertThrows(UnauthorizedAccessException.class,
                () -> orderHistoryService.findById(order.getId()));

        verifyAuthentication();
        verify(orderHistoryRepository, times(1)).findById(order.getId());
    }

    @Test
    void givenPaging_whenFindByCustomer_ThenReturnOrderHistoryPage() {
        when(orderHistoryRepository.findAllByCustomer(eq(customer),any(Pageable.class)))
                .thenReturn(orderHistoryPage);

        Page<OrderHistory> result = orderHistoryService
                .findByCurrentUser(orderHistoryPage.getPageable().getPageNumber(),
                        orderHistoryPage.getPageable().getPageSize(),
                        orderHistoryPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(orderHistoryPage, result);

        verify(orderHistoryRepository, times(1))
                .findAllByCustomer(customer, orderHistoryPage.getPageable());
    }


}