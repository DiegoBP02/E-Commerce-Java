package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.entities.Order;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.enums.OrderStatus;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class OrderIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/orders";

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
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    public OrderIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Admin admin = new Admin("admin", "admin", "admin");
    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Order order = TestDataBuilder.buildOrder(customer);

    private void insertCustomer() {
        userRepository.save(customer);
    }

    private void insertOrder() {
        insertCustomer();
        orderRepository.save(order);
    }

    Customer setupCustomer() {
        return (Customer) userRepository.findByEmail(customer.getEmail())
                .orElseGet(() -> userRepository.save(customer));
    }

    Customer setupCustomerWithOrder() {
        customer.setOrders(Collections.singletonList(order));
        orderRepository.save(order);
        return userRepository.save(customer);
    }

    Admin setupAdmin() {
        return (Admin) userRepository.findByEmail(admin.getEmail())
                .orElseGet(() -> userRepository.save(admin));
    }

    @Test
    void givenValidBodyAndCustomer_whenCreate_thenReturnOrderAndCreated() throws Exception {
        mockMvc.perform(mockPostRequest().with(user(setupCustomer())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(order.getTotalAmount()));

        assertEquals(1, orderRepository.findAll().size());
    }

    @Test
    void givenOrderAndAdmin_whenFindAll_thenReturnOrders() throws Exception {
        insertOrder();

        mockMvc.perform(mockGetRequest().with(user(setupAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalAmount").value(order.getTotalAmount()));
    }

    @Test
    void givenOrderAndCustomer_whenFindById_thenReturnOrder() throws Exception {
        insertOrder();

        mockMvc.perform(mockGetRequest(order.getId().toString()).with(user(setupCustomerWithOrder())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(order.getTotalAmount()));
    }

    @Test
    void givenOrderAndCustomer_whenFindActiveOrderByCurrentUser_thenReturnOrder() throws Exception {
        order.setStatus(OrderStatus.Active);
        insertOrder();

        mockMvc.perform(mockGetRequest("user").with(user(setupCustomerWithOrder())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(order.getTotalAmount()));
    }

    @Test
    void givenOrderAndCustomer_whenDelete_thenReturnNoContentAndDeleteOrder() throws Exception {
        insertOrder();

        MockHttpServletRequestBuilder mockRequest
                = mockDeleteRequest(order.getId().toString()).with(user(setupCustomerWithOrder()));

        mockMvc.perform(mockRequest).andExpect(status().isNoContent());

        assertEquals(0, orderRepository.findAll().size());
    }

    @Test
    void givenOrderAndCustomer_whenDeleteByCurrentUser_thenReturnNoContentAndDeleteOrder() throws Exception {
        insertOrder();

        MockHttpServletRequestBuilder mockRequest
                = mockDeleteRequest("user").with(user(setupCustomerWithOrder()));

        mockMvc.perform(mockRequest).andExpect(status().isNoContent());

        assertEquals(0, orderRepository.findAll().size());
    }

}