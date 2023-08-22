package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.Order;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.exceptions.UniqueConstraintViolationError;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderServiceTest extends ApplicationConfigTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Product product = TestDataBuilder.buildProduct(seller);
    private Customer customer = TestDataBuilder.buildCustomer();
    private Order order = TestDataBuilder.buildOrder(customer);

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
    void givenValidUser_whenCreate_thenReturnOrder() {
        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        Order result = orderService.create();

        assertEquals(order, result);

        verifyAuthentication();
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void givenValidUserAndOrderAlreadyExists_whenCreate_thenThrowUniqueConstraintViolationError() {
        when(orderRepository.save(any(Order.class)))
                .thenThrow(DataIntegrityViolationException.class);

        assertThrows(UniqueConstraintViolationError.class, () ->
                orderService.create());

        verifyAuthentication();
        verify(orderRepository, times(1)).save(any(Order.class));
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
    void givenOrder_whenFindByCurrentUser_thenReturnOrder() {
        when(orderRepository.findByCustomerId(customer.getId())).thenReturn(Optional.of(order));

        Order result = orderService.findByCurrentUser();

        assertEquals(order, result);

        verifyAuthentication();
        verify(orderRepository, times(1)).findByCustomerId(customer.getId());
    }

    @Test
    void givenNoOrder_whenFindByCurrentUser_thenThrowResourceNotFoundException() {
        when(orderRepository.findByCustomerId(customer.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.findByCurrentUser());

        verifyAuthentication();
        verify(orderRepository, times(1)).findByCustomerId(customer.getId());
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

}