package com.example.demo.IntegrationTests;

import com.example.demo.controller.ApplicationConfigTestController;
import com.example.demo.dtos.ProductDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Seller;
import com.example.demo.repositories.ProductRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class ProductIntegrationTest extends ApplicationConfigTestController {

    private static final String PATH = "/products";

    @Container
    static PostgreSQLContainer postgreSQLContainer = BasePostgresqlContainer.getInstance();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;

    public ProductIntegrationTest() {
        super(PATH);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Seller seller = (Seller) TestDataBuilder.buildUserNoId();
    private Product product = TestDataBuilder.buildProductNoId(seller);
    private ProductDTO productDTO = TestDataBuilder.buildProductDTO();

    private void insertSeller() {
        userRepository.save(seller);
    }

    private void insertProduct() {
        insertSeller();
        productRepository.save(product);
    }

    Seller setupSeller() {
        seller.setEnabled(true);
        return userRepository.save(seller);
    }

    @Test
    void givenValidBodyAndSeller_whenCreate_thenReturnProductAndCreated() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(productDTO).with(user(setupSeller()));

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(productDTO.getName()));

        assertEquals(1, productRepository.findAll().size());
    }

    @Test
    void givenProductsAndNoUser_whenFindAll_thenReturnProductPage() throws Exception {
        insertProduct();

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenProductsAndNoUser_whenFindByCategory_thenReturnProduct() throws Exception {
        insertProduct();

        MockHttpServletRequestBuilder mockRequest = mockGetRequestWithParams
                ("/category", "productCategory", product.getCategory().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(product.getName()));
    }

    @Test
    void givenProductAndNoUser_whenFindById_thenReturnProduct() throws Exception {
        insertProduct();

        mockMvc.perform(mockGetRequest(product.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(product.getName()));
    }

    @Test
    void givenProductAndSeller_whenFindByCurrentUser_thenReturnProductPage() throws Exception {
        insertProduct();

        mockMvc.perform(mockGetRequest("user").with(user(setupSeller())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenProductAndSeller_whenFindBySellerId_thenReturnProductPage() throws Exception {
        insertProduct();

        mockMvc.perform(mockGetRequest("seller/" + seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void givenValidBodyAndSeller_whenUpdate_thenReturnProduct() throws Exception {
        insertProduct();

        MockHttpServletRequestBuilder mockRequest
                = mockPatchRequest(product.getId().toString(), productDTO).with(user(setupSeller()));

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(productDTO.getName()));
    }

    @Test
    void givenProductAndSeller_whenDelete_thenReturnNoContentAndDeleteProduct() throws Exception {
        insertProduct();

        MockHttpServletRequestBuilder mockRequest
                = mockDeleteRequest(product.getId().toString()).with(user(setupSeller()));

        mockMvc.perform(mockRequest).andExpect(status().isNoContent());

        assertEquals(0, productRepository.findAll().size());
    }
}