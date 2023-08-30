package com.example.demo.repositories;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OrderHistoryRepositoryTest {

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Order order;
    private OrderHistory orderHistory;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(customer);
        order = TestDataBuilder.buildOrder(customer);
        orderHistory = TestDataBuilder.buildOrderHistory(order);
    }

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
    }

    @Test
    void givenOrderHistory_whenFindAllByCustomer_thenReturnOrderHistoryPage() {
        Pageable paging = PageRequest.of(0, 5, Sort.by("paymentDate"));
        orderHistoryRepository.save(orderHistory);

        List<OrderHistory> orderHistoryList = Collections.singletonList(orderHistory);
        Page<OrderHistory> expectedResult = new PageImpl<>(orderHistoryList, paging, orderHistoryList.size());

        Page<OrderHistory> result = orderHistoryRepository.findAllByCustomer(customer, paging);
        assertEquals(expectedResult, result);
    }

}