package com.example.demo.repositories;

import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import com.example.demo.enums.OrderStatus;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Order order;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(customer);
        order = TestDataBuilder.buildOrder(customer);
    }

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
    }

    @Test
    void givenOrder_whenFindByCustomerId_thenReturnOptionalOrder() {
        orderRepository.save(order);
        Optional<Order> result = orderRepository.findByCustomerId(customer.getId());
        assertEquals(Optional.of(order), result);
    }

    @Test
    void givenNoOrder_whenFindByCustomerId_thenReturnOptionalEmpty() {
        Optional<Order> result = orderRepository.findByCustomerId(UUID.randomUUID());
        assertEquals(Optional.empty(), result);
    }

    @Test
    void givenOrder_whenFindActiveOrderByCustomer_thenReturnOptionalOrder() {
        order.setStatus(OrderStatus.Active);
        orderRepository.save(order);
        Optional<Order> result = orderRepository.findActiveOrderByCurrentUser(customer);
        assertEquals(Optional.of(order), result);
    }

    @Test
    void givenNoOrder_whenFindActiveOrderByCustomer_thenReturnOptionalEmpty() {
        Optional<Order> result = orderRepository.findActiveOrderByCurrentUser(customer);
        assertEquals(Optional.empty(), result);
    }



}