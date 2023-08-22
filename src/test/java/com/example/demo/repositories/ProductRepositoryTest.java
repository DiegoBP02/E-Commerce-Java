package com.example.demo.repositories;

import com.example.demo.entities.Product;
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
class ProductRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private Seller seller = (Seller) TestDataBuilder.buildUser();
    private Product product = TestDataBuilder.buildProductNoId(seller);

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(seller);
    }

    @AfterEach
    void tearDown() throws Exception {
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void givenProducts_whenFindByCategory_thenReturnProducts() {
        productRepository.save(product);
        List<Product> result = productRepository.findByCategory(product.getCategory());
        assertEquals(Collections.singletonList(product), result);
    }

}