package com.example.demo.repositories;

import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private Seller seller = (Seller) TestDataBuilder.buildUser();
    private Customer customer = TestDataBuilder.buildCustomerNoId();
    private Product product;
    private Review review;
    Pageable paging = PageRequest.of(0, 5, Sort.by("rating"));

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(seller);
        userRepository.save(customer);
        product = TestDataBuilder.buildProductNoId(seller);
        productRepository.save(product);
        review = TestDataBuilder.buildReviewNoId(product,customer);
    }

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
    }

    @Test
    void givenReviews_whenFindAllByProduct_thenReturnReviewPage() {
        reviewRepository.save(review);

        List<Review> reviewList = Collections.singletonList(review);
        Page<Review> expectedResult = new PageImpl<>(reviewList, paging, reviewList.size());

        Page<Review> result = reviewRepository.findAllByProduct(product, paging);
        assertEquals(expectedResult, result);
    }

    @Test
    void givenReviews_whenFindAllByCustomer_thenReturnReviewPage() {
        reviewRepository.save(review);

        List<Review> reviewList = Collections.singletonList(review);
        Page<Review> expectedResult = new PageImpl<>(reviewList, paging, reviewList.size());

        Page<Review> result = reviewRepository.findAllByCustomer(customer, paging);
        assertEquals(expectedResult, result);
    }
}