package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.dtos.OrderItemDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.repositories.OrderItemRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class OrderItemIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/orderItems";

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
    @Autowired
    private OrderItemRepository orderItemRepository;

    public OrderItemIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        productRepository.deleteAll();
        orderRepository.deleteAll();
        orderItemRepository.deleteAll();
    }

    private Admin admin = new Admin("admin", "admin", "admin");
    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Seller seller = (Seller) TestDataBuilder.buildUserNoId();
    private Product product = TestDataBuilder.buildProductNoId(seller);
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderItem orderItem = TestDataBuilder.buildOrderItemNoId(order, product);
    private OrderItemDTO orderItemDTO = TestDataBuilder.buildOrderItemDTO();

    private void insertCustomer() {
        userRepository.save(customer);
    }

    private void insertSeller() {
        userRepository.save(seller);
    }

    private void insertOrder() {
        insertCustomer();
        orderRepository.save(order);
    }

    private void insertProduct(){
        insertSeller();
        productRepository.save(product);
    }

    private void insertOrderItem(){
        insertOrder();
        insertProduct();
        orderItemRepository.save(orderItem);
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
        return userRepository.save(admin);
    }

    @Test
    void givenValidBodyAndCustomer_whenCreate_thenReturnOrderItemAndCreated() throws Exception {
        insertProduct();
        orderItemDTO.setProductId(product.getId());

        mockMvc.perform(mockPostRequest(orderItemDTO).with(user(setupCustomer())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(orderItemDTO.getQuantity()));

        assertEquals(1, orderItemRepository.findAll().size());
    }

    @Test
    void givenOrderItemAndAdmin_whenFindAll_thenReturnOrderItems() throws Exception {
        insertOrderItem();

        mockMvc.perform(mockGetRequest().with(user(setupAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(orderItem.getQuantity()));
    }

    @Test
    void givenOrderItemAndCustomer_whenFindByOrderId_thenReturnOrderItems() throws Exception {
        insertOrderItem();

        MockHttpServletRequestBuilder mockRequest
                = mockGetRequest("order/" + order.getId().toString())
                .with(user(setupCustomerWithOrder()));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(orderItem.getQuantity()));
    }

    @Test
    void givenOrderItemAndCustomer_whenFindById_thenReturnOrderItem() throws Exception {
        insertOrderItem();

        mockMvc.perform(mockGetRequest(orderItem.getId().toString()).with(user(setupCustomerWithOrder())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(orderItem.getQuantity()));
    }

    @Test
    void givenValidBodyAndCustomer_whenUpdate_thenReturnUpdatedOrderItem() throws Exception {
        insertOrderItem();
        orderItemDTO.setQuantity(5);

        MockHttpServletRequestBuilder mockRequest
                = mockPatchRequest(orderItem.getId().toString(), orderItemDTO)
                .with(user(setupCustomerWithOrder()));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(orderItemDTO.getQuantity()));
    }

    @Test
    void givenOrderItemAndCustomer_whenDelete_thenReturnNoContentAndDeleteOrderItem() throws Exception {
        insertOrderItem();

        MockHttpServletRequestBuilder mockRequest
                = mockDeleteRequest(orderItem.getId().toString()).with(user(setupCustomerWithOrder()));

        mockMvc.perform(mockRequest).andExpect(status().isNoContent());

        assertEquals(0, orderItemRepository.findAll().size());
    }

}