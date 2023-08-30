package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderServiceTest extends ApplicationConfigTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private OrderHistoryService orderHistoryService;

    @MockBean
    private UserRepository userRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Customer customer = TestDataBuilder.buildCustomer();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderItem mockOrderItem = mock(OrderItem.class);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistory(order);

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyNoAuthentication() {
        verify(authentication, never()).getPrincipal();
        verify(securityContext, never()).getAuthentication();
    }

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenValidUserAndNoExistingActiveOrder_whenCreate_thenReturnActiveOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            assertEquals(OrderStatus.Active, savedOrder.getStatus());
            return order;
        });

        Order result = orderService.create();

        assertEquals(order, result);

        verifyAuthentication();
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenValidUserAndExistingActiveOrder_whenCreate_thenThrowActiveOrderAlreadyExistsException() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.of(order));

        assertThrows(ActiveOrderAlreadyExistsException.class, () ->
                orderService.create());

        verifyAuthentication();
        verify(orderRepository, never()).save(any(Order.class));
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenValidUserAndOrderAlreadyExists_whenCreate_thenThrowUniqueConstraintViolationError() {
        when(orderRepository.save(any(Order.class)))
                .thenThrow(DataIntegrityViolationException.class);

        assertThrows(UniqueConstraintViolationError.class, () ->
                orderService.create());

        verifyAuthentication();
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenOrders_whenFindAll_ThenReturnOrder() {
        List<Order> orders = Collections.singletonList(order);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> result = orderService.findAll();

        assertEquals(orders, result);

        verifyNoAuthentication();
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void givenOrder_whenFindById_thenReturnOrder() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Order result = orderService.findById(order.getId());

        assertEquals(order, result);

        verifyNoAuthentication();
        verify(orderRepository, times(1)).findById(order.getId());
    }

    @Test
    void givenNoOrder_whenFindById_thenThrowResourceNotFoundException() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findById(order.getId()));

        verifyNoAuthentication();
        verify(orderRepository, times(1)).findById(order.getId());
    }

    @Test
    void givenOrder_whenFindActiveOrderByCurrentUser_thenReturnOrder() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.of(order));

        Order result = orderService.findActiveOrderByCurrentUser();

        assertEquals(order, result);

        verifyAuthentication();
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenNoOrder_whenFindActiveOrderByCurrentUser_thenThrowResourceNotFoundException() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findActiveOrderByCurrentUser());

        verifyAuthentication();
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenOrder_whenDelete_thenDeleteOrder() {
        when(orderRepository.getReferenceById(order.getId())).thenReturn(order);

        orderService.delete(order.getId());

        verifyAuthentication();
        verify(orderRepository, times(1)).getReferenceById(order.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void givenNoOrder_whenDelete_thenThrowResourceNotFoundException() {
        when(orderRepository.getReferenceById(order.getId())).thenReturn(order);
        doThrow(EmptyResultDataAccessException.class)
                .when(orderRepository).deleteById(order.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.delete(order.getId()));

        verifyAuthentication();
        verify(orderRepository, times(1)).getReferenceById(order.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void givenOrderAndDeleteCausesDataIntegrityViolationException_whenDelete_thenThrowDatabaseException() {
        when(orderRepository.getReferenceById(order.getId())).thenReturn(order);
        doThrow(DataIntegrityViolationException.class)
                .when(orderRepository).deleteById(order.getId());

        assertThrows(DatabaseException.class,
                () -> orderService.delete(order.getId()));

        verifyAuthentication();
        verify(orderRepository, times(1)).getReferenceById(order.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void givenOrderDoesNotBelongToUser_whenDelete_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(orderRepository.getReferenceById(order.getId())).thenReturn(order);

        assertThrows(UnauthorizedAccessException.class,
                () -> orderService.delete(order.getId()));

        verifyAuthentication();
        verify(orderRepository, times(1)).getReferenceById(order.getId());
        verify(orderRepository, never()).deleteById(order.getId());
    }

    @Test
    void givenOrder_whenDeleteByCurrentUser_thenDeleteOrder() {
        when(orderRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(order));

        orderService.deleteByCurrentUser();

        verifyAuthentication();
        verify(orderRepository, times(1)).findByCustomerId(customer.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void givenNoOrder_whenDeleteByCurrentUser_thenThrowResourceNotFoundException() {
        when(orderRepository.findByCustomerId(customer.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.deleteByCurrentUser());

        verifyAuthentication();
        verify(orderRepository, times(1)).findByCustomerId(customer.getId());
        verify(orderRepository, never()).deleteById(order.getId());
    }

    @Test
    void givenOrderAndDeleteCausesDataIntegrityViolationException_whenDeleteByCurrentUser_thenThrowDatabaseException() {
        when(orderRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(order));
        doThrow(DataIntegrityViolationException.class)
                .when(orderRepository).deleteById(order.getId());

        assertThrows(DatabaseException.class,
                () -> orderService.deleteByCurrentUser());

        verifyAuthentication();
        verify(orderRepository, times(1)).findByCustomerId(customer.getId());
        verify(orderRepository, times(1)).deleteById(order.getId());
    }

    @Test
    void givenOrderWithItems_whenCheckUserOrder_thenDoNothing() {
        order.setItems(Collections.singletonList(mockOrderItem));
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.of(order));

        orderService.checkUserOrder();

        verifyAuthentication();
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }


    @Test
    void givenNoOrder_whenCheckUserOrder_thenThrowResourceNotFoundException() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.checkUserOrder()
        );

        verifyAuthentication();
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenOrderWithNoItems_whenCheckUserOrder_thenThrowInvalidOrderExceptionWithCorrectMessage() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.of(order));

        InvalidOrderException invalidOrderException = assertThrows(InvalidOrderException.class, () ->
                orderService.checkUserOrder()
        );

        assertEquals("Invalid order: The order does not contain any items",
                invalidOrderException.getMessage());

        verifyAuthentication();
        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
    }

    @Test
    void givenCreditCardAndPaymentAmount_whenMoveOrderToHistory_thenAddOrderHistoryAndSetOrderStatusDelivered() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.of(order));
        when(orderHistoryService.create(any(OrderHistoryDTO.class))).thenReturn(orderHistory);

        int orderHistoryLength = customer.getOrderHistory().size();

        orderService.moveOrderToHistory(CreditCard.pm_card_visa, BigDecimal.ONE);

        assertEquals(OrderStatus.Delivered, order.getStatus());
        assertEquals(orderHistoryLength + 1, customer.getOrderHistory().size());

        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
        verify(orderHistoryService, times(1)).create(any(OrderHistoryDTO.class));
        verify(userRepository, times(1)).save(customer);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void givenNoActiveOrder_whenMoveOrderToHistory_thenThrowResourceNotFoundException() {
        when(orderRepository.findActiveOrderByCurrentUser(customer)).thenReturn(Optional.empty());
        when(orderHistoryService.create(any(OrderHistoryDTO.class))).thenReturn(orderHistory);

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.moveOrderToHistory(CreditCard.pm_card_visa, BigDecimal.ONE));

        verify(orderRepository, times(1)).findActiveOrderByCurrentUser(customer);
        verify(orderHistoryService, never()).create(any(OrderHistoryDTO.class));
        verify(userRepository, never()).save(customer);
        verify(orderRepository, never()).save(order);
    }

}