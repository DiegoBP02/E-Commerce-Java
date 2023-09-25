package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.dtos.ProductDTO;
import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.*;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.repositories.*;
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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class ReviewIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/reviews";

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
    private ReviewRepository reviewRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderHistoryRepository orderHistoryRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    public ReviewIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Seller seller = (Seller) TestDataBuilder.buildUserNoId();
    private Admin admin = new Admin("admin", "admin", "admin");
    private Product product = TestDataBuilder.buildProductNoId(seller);
    private ProductDTO productDTO = TestDataBuilder.buildProductDTO();
    private Review review = TestDataBuilder.buildReviewNoId(product, customer);
    private ReviewDTO reviewDTO = TestDataBuilder.buildReviewDTO();
    private UpdateReviewDTO updateReviewDTO = TestDataBuilder.buildUpdateReviewDTO();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderItem orderItem = TestDataBuilder.buildOrderItemNoId(order,product);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistory(order);

    private void insertCustomer() {
        userRepository.save(customer);
    }

    private void insertSeller() {
        userRepository.save(seller);
    }

    private void insertProduct(){
        insertSeller();
        productRepository.save(product);
    }

    private void insertProductWithOrderHistory(){
        insertProduct();
        order.setItems(Collections.singletonList(orderItem));
        insertOrder();
        insertOrderItem();
        orderHistory.setOrder(order);
        insertOrderHistory();
    }

    private void insertOrderItem(){
        orderItemRepository.save(orderItem);
    }

    private void insertReview(){
        insertProduct();
        customer.setReviews(Collections.singletonList(review));
        insertCustomer();
        reviewRepository.save(review);
    }

    private void insertOrder() {
        insertCustomer();
        orderRepository.save(order);
    }

    private void insertOrderHistory(){
        orderHistoryRepository.save(orderHistory);
    }

    Customer setupCustomer() {
        return (Customer) userRepository.findByEmail(customer.getEmail())
                .orElseGet(() -> userRepository.save(customer));
    }

    Admin setupAdmin() {
        return (Admin) userRepository.findByEmail(admin.getEmail())
                .orElseGet(() -> userRepository.save(admin));
    }

    @Test
    @Transactional
    void givenValidBodyAndCustomer_whenCreate_thenReturnReviewAndCreated() throws Exception {
        insertProductWithOrderHistory();
        reviewDTO.setProductId(product.getId());

        mockMvc.perform(mockPostRequest(reviewDTO).with(user(setupCustomer())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.comment").value(review.getComment()));

        assertEquals(1, reviewRepository.findAll().size());
    }

    @Test
    void givenReviewsAndAdmin_whenFindAll_thenReturnReviewPage() throws Exception {
        insertReview();

        mockMvc.perform(mockGetRequest().with(user(setupAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenReviewAndNoUser_whenFindById_thenReturnReview() throws Exception {
        insertReview();

        mockMvc.perform(mockGetRequest(review.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value(review.getComment()));
    }

    @Test
    void givenReviewAndNoUser_whenFindAllByProduct_thenReturnReviewPage() throws Exception {
        insertReview();

        mockMvc.perform(mockGetRequest("product/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenReviewAndCustomer_whenFindByCurrentUser_thenReturnReviewPage() throws Exception {
        insertReview();

        mockMvc.perform(mockGetRequest("user").with(user(setupCustomer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenValidBodyAndCustomer_whenUpdate_thenReturnUpdatedReview() throws Exception {
        insertReview();
        updateReviewDTO.setComment("random");

        MockHttpServletRequestBuilder mockRequest
                = mockPatchRequest(review.getId().toString(), updateReviewDTO).with(user(setupCustomer()));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value(updateReviewDTO.getComment()));
    }

    @Test
    void givenReviewAndCustomer_whenDelete_thenReturnNoContentAndDeleteReview() throws Exception {
        insertReview();

        MockHttpServletRequestBuilder mockRequest
                = mockDeleteRequest(review.getId().toString()).with(user(setupCustomer()));

        mockMvc.perform(mockRequest).andExpect(status().isNoContent());

        assertEquals(0, reviewRepository.findAll().size());
    }
}