package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewServiceTest extends ApplicationConfigTest {
    @Autowired
    private ReviewService reviewService;

    @MockBean
    private ReviewRepository reviewRepository;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderHistoryService orderHistoryService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Customer customer = TestDataBuilder.buildCustomerWithId();
    private Product product = TestDataBuilder.buildProductWithId(seller);
    private Review review = TestDataBuilder.buildReviewWithId(product, customer);
    private ReviewDTO reviewDTO = TestDataBuilder.buildReviewDTO();
    private Page<Review> reviewPage = TestDataBuilder.buildPage(review,0,5,"rating");
    private UpdateReviewDTO updateReviewDTO = TestDataBuilder.buildUpdateReviewDTO();

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyNoAuthentication() {
        verify(authentication, never()).getPrincipal();
        verify(securityContext, never()).getAuthentication();
    }

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenReviewDTOAndUserProductPurchasedByUser_whenCreate_thenReturnReview() {
        when(productService.findById(reviewDTO.getProductId())).thenReturn(product);
        when(orderHistoryService.isProductPurchasedByUser(product)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        Review result = reviewService.create(reviewDTO);

        assertEquals(review, result);

        verifyAuthentication();
        verify(productService, times(1)).findById(reviewDTO.getProductId());
        verify(orderHistoryService, times(1)).isProductPurchasedByUser(product);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void givenReviewDTOAndProductWasNotPurchasedByUser_whenCreate_thenThrowProductNotPurchasedException() {
        when(productService.findById(reviewDTO.getProductId())).thenReturn(product);
        when(orderHistoryService.isProductPurchasedByUser(product)).thenReturn(false);

        assertThrows(ProductNotPurchasedException.class,
                () -> reviewService.create(reviewDTO));

        verifyNoAuthentication();
        verify(productService, times(1)).findById(reviewDTO.getProductId());
        verify(orderHistoryService, times(1)).isProductPurchasedByUser(product);
        verifyNoInteractions(reviewRepository);
    }

    @Test
    void givenReviewForProductAlreadyExists_whenCreate_thenHandleDataIntegrityViolationException() {
        when(productService.findById(reviewDTO.getProductId())).thenReturn(product);
        when(orderHistoryService.isProductPurchasedByUser(product)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(UniqueConstraintViolationError.class,
                () -> reviewService.create(reviewDTO));

        verifyAuthentication();
        verify(productService, times(1)).findById(reviewDTO.getProductId());
        verify(orderHistoryService, times(1)).isProductPurchasedByUser(product);
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void givenProductIdAndPaging_whenFindAllByCustomer_ThenReturnReviewPage() {
        when(productService.findById(product.getId())).thenReturn(product);
        when(reviewRepository.findAllByProduct(product, reviewPage.getPageable()))
                .thenReturn(reviewPage);

        Page<Review> result = reviewService
                .findAllByProduct(product.getId(),
                        reviewPage.getPageable().getPageNumber(),
                        reviewPage.getPageable().getPageSize(),
                        reviewPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(reviewPage, result);

        verify(reviewRepository, times(1))
                .findAllByProduct(product, reviewPage.getPageable());
    }

    @Test
    void givenPaging_whenFindByCurrentUser_ThenReturnReviewPage() {
        when(reviewRepository.findAllByCustomer(customer, reviewPage.getPageable()))
                .thenReturn(reviewPage);

        Page<Review> result = reviewService
                .findByCurrentUser(reviewPage.getPageable().getPageNumber(),
                        reviewPage.getPageable().getPageSize(),
                        reviewPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(reviewPage, result);

        verify(reviewRepository, times(1))
                .findAllByCustomer(customer, reviewPage.getPageable());
    }

    @Test
    void givenReviews_whenFindAll_ThenReturnReviewPage() {
        when(reviewRepository.findAll(any(Pageable.class))).thenReturn(reviewPage);

        Page<Review> result = reviewService
                .findAll(reviewPage.getPageable().getPageNumber(),
                        reviewPage.getPageable().getPageSize(),
                        reviewPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(reviewPage, result);

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findAll(reviewPage.getPageable());
    }

    @Test
    void givenReview_whenFindById_thenReturnReview() {
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.of(review));

        Review result = reviewService.findById(review.getId());

        assertEquals(review, result);

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findById(review.getId());
    }

    @Test
    void givenNoReview_whenFindById_thenThrowResourceNotFoundException() {
        when(reviewRepository.findById(review.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.findById(review.getId()));

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findById(review.getId());
    }

    @Test
    void givenValidIdAndReviewDTO_whenUpdate_thenReturnUpdatedReview() {
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);

        Review result = reviewService.update(review.getId(), updateReviewDTO);

        assertEquals(updateReviewDTO.getRating(), result.getRating());

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void givenNoReview_whenUpdate_thenThrowResourceNotFoundException() {
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);
        when(reviewRepository.save(review))
                .thenThrow(EntityNotFoundException.class);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.update(review.getId(), updateReviewDTO));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void givenReviewDoesNotBelongToUser_whenUpdate_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);

        assertThrows(UnauthorizedAccessException.class,
                () -> reviewService.update(review.getId(), updateReviewDTO));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, never()).save(review);
    }

    @Test
    void givenReview_whenDelete_thenDeleteReview() {
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);

        reviewService.delete(review.getId());

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).deleteById(review.getId());
    }

    @Test
    void givenNoReview_whenDelete_thenThrowResourceNotFoundException() {
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);
        doThrow(EmptyResultDataAccessException.class)
                .when(reviewRepository).deleteById(review.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.delete(review.getId()));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).deleteById(review.getId());
    }

    @Test
    void givenReviewAndDeleteCausesDataIntegrityViolationException_whenDelete_thenThrowDatabaseException() {
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);
        doThrow(DataIntegrityViolationException.class)
                .when(reviewRepository).deleteById(review.getId());

        assertThrows(DatabaseException.class,
                () -> reviewService.delete(review.getId()));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).deleteById(review.getId());
    }

    @Test
    void givenReviewDoesNotBelongToUser_whenDelete_thenThrowUnauthorizedAccessException() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Seller.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();

        when(authentication.getPrincipal()).thenReturn(user2);
        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);

        assertThrows(UnauthorizedAccessException.class,
                () -> reviewService.delete(review.getId()));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, never()).deleteById(review.getId());
    }

    @Test
    void givenReviewAndCurrentUserIsAdmin_whenDelete_thenDeleteReview() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Admin.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();
        when(authentication.getPrincipal()).thenReturn(user2);

        when(reviewRepository.getReferenceById(review.getId())).thenReturn(review);

        reviewService.delete(review.getId());

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(review.getId());
        verify(reviewRepository, times(1)).deleteById(review.getId());
    }

}