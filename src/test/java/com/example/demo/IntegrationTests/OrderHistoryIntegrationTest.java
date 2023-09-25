package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.repositories.OrderHistoryRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class OrderHistoryIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/orderHistory";

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    public OrderHistoryIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistory(order);

    private void insertCustomer() {
        userRepository.save(customer);
    }

    private void insertOrderHistory() {
        insertCustomer();
        orderHistoryRepository.save(orderHistory);
    }

    Customer setupCustomer() {
        return (Customer) userRepository.findByEmail(customer.getEmail())
                .orElseGet(() -> userRepository.save(customer));
    }

    @Test
    void givenOrderHistoryAndCustomer_whenFindById_thenReturnOrderHistory() throws Exception {
        insertOrderHistory();

        mockMvc.perform(mockGetRequest(orderHistory.getId().toString()).with(user(setupCustomer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentAmount").value(orderHistory.getPaymentAmount().floatValue()));
    }

    @Test
    void givenOrderHistoryAndCustomer_whenFindByCurrentUser_thenReturnOrderHistoryPage() throws Exception {
        insertOrderHistory();

        mockMvc.perform(mockGetRequest("user").with(user(setupCustomer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

}