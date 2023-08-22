package com.example.demo.repositories;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class OrderItemRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Seller seller = (Seller) TestDataBuilder.buildUser();
    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(seller);
        userRepository.save(customer);

        order = TestDataBuilder.buildOrder(customer);
        orderRepository.save(order);

        product = TestDataBuilder.buildProductNoId(seller);
        productRepository.save(product);

        orderItem = TestDataBuilder.buildOrderItem(order, product);
    }

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
    }

    @Test
    void givenOrderItems_whenFindByOrderId_thenReturnOrderItems() {
        orderItemRepository.save(orderItem);
        List<OrderItem> result = orderItemRepository.findByOrderId(orderItem.getOrder().getId());
        assertEquals(Collections.singletonList(orderItem), result);
    }

}