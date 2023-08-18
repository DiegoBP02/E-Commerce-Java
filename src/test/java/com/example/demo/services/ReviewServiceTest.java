package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.services.utils.CheckOwnership;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ReviewServiceTest extends ApplicationConfigTest {
    @Autowired
    ReviewService reviewService;

    @MockBean
    ReviewRepository reviewRepository;

    @MockBean
    private ProductService productService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Customer customer = TestDataBuilder.buildCustomer();
    private Product product = TestDataBuilder.buildProduct(seller);
    private Review review = TestDataBuilder.buildReview(product, customer);
    private ReviewDTO reviewDTO = TestDataBuilder.buildReviewDTO();
    private Page<Review> reviewPage = new PageImpl<>
            (Collections.singletonList(review),
                    PageRequest.of(0, 5, Sort.by("rating")), 1);
    private UpdateReviewDTO updateReviewDTO = TestDataBuilder.buildUpdateReviewDTO();

    UUID reviewId = review.getId();

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private void verifyNoAuthentication(){
        verify(authentication, never()).getPrincipal();
        verify(securityContext, never()).getAuthentication();
    }

    private void verifyAuthentication(){
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenValidReviewDTO_whenCreate_thenReturnReview() {
        when(productService.findById(reviewDTO.getProductId())).thenReturn(product);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        Review result = reviewService.create(reviewDTO);

        assertEquals(review,result);

        verifyAuthentication();
        verify(productService, times(1)).findById(reviewDTO.getProductId());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void givenValidReviewDTOAndProductDoesNotExists_whenCreate_thenThrowResourceNotFoundException() {
        when(productService.findById(reviewDTO.getProductId()))
                .thenThrow(ResourceNotFoundException.class);
        when(reviewRepository.save(any(Review.class))).thenReturn(review);

        assertThrows(ResourceNotFoundException.class, () ->
                reviewService.create(reviewDTO));

        verify(productService, times(1)).findById(reviewDTO.getProductId());
        verifyNoAuthentication();
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void givenReviews_whenFindAll_ThenReturnReviewPage() {
        when(reviewRepository.findAll(any(Pageable.class))).thenReturn(reviewPage);

        Page<Review> result = reviewService
                .findAll(reviewPage.getPageable().getPageNumber(),
                        reviewPage.getPageable().getPageSize(),
                        reviewPage.getPageable().getSort().stream().toList().get(0).getProperty());

        assertEquals(reviewPage,result);

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findAll(reviewPage.getPageable());
    }

    @Test
    void givenReview_whenFindById_thenReturnReview() {
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        Review result = reviewService.findById(reviewId);

        assertEquals(review,result);

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findById(reviewId);
    }

    @Test
    void givenNoReview_whenFindById_thenThrowResourceNotFoundException() {
        when(reviewRepository.findById(reviewId)).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.findById(reviewId));

        verifyNoAuthentication();
        verify(reviewRepository, times(1)).findById(reviewId);
    }

    @Test
    void givenValidIdAndReviewDTO_whenUpdate_thenReturnUpdatedReview() {
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);

        Review result = reviewService.update(reviewId, updateReviewDTO);

        assertEquals(updateReviewDTO.getRating(),result.getRating());

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void givenNoReview_whenUpdate_thenThrowResourceNotFoundException(){
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);
        when(reviewRepository.save(review))
                .thenThrow(EntityNotFoundException.class);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.update(reviewId, updateReviewDTO));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, times(1)).save(review);
    }

    @Test
    void givenReviewDoesNotBelongToUser_whenUpdate_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(review);

        assertThrows(UnauthorizedAccessException.class,
                () -> reviewService.update(reviewId, updateReviewDTO));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, never()).save(review);
    }

    @Test
    void givenReview_whenDelete_thenDeleteReview() {
        when(reviewRepository.getReferenceById(any(UUID.class))).thenReturn(review);

        reviewService.delete(UUID.randomUUID());

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(any(UUID.class));
        verify(reviewRepository, times(1)).deleteById(any(UUID.class));
    }

    @Test
    void givenNoReview_whenDelete_thenThrowResourceNotFoundException(){
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);
        doThrow(EmptyResultDataAccessException.class)
                .when(reviewRepository).deleteById(reviewId);

        assertThrows(ResourceNotFoundException.class,
                () -> reviewService.delete(reviewId));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, times(1)).deleteById(reviewId);
    }

    @Test
    void givenReviewAndDeleteCausesDataIntegrityViolationException_whenDelete_thenThrowDatabaseException(){
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);
        doThrow(DataIntegrityViolationException.class)
                .when(reviewRepository).deleteById(reviewId);

        assertThrows(DatabaseException.class,
                () -> reviewService.delete(reviewId));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, times(1)).deleteById(reviewId);
    }

    @Test
    void givenReviewDoesNotBelongToUser_whenDelete_thenThrowUnauthorizedAccessException() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Seller.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();

        when(authentication.getPrincipal()).thenReturn(user2);
        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);

        assertThrows(UnauthorizedAccessException.class,
                () -> reviewService.delete(reviewId));

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, never()).deleteById(reviewId);
    }

    @Test
    void givenReviewAndCurrentUserIsAdmin_whenDelete_thenDeleteReview() {
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(Role.Admin.name());
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        doReturn(authorities).when(user2).getAuthorities();
        when(authentication.getPrincipal()).thenReturn(user2);

        when(reviewRepository.getReferenceById(reviewId)).thenReturn(review);

        reviewService.delete(reviewId);

        verifyAuthentication();
        verify(reviewRepository, times(1)).getReferenceById(reviewId);
        verify(reviewRepository, times(1)).deleteById(reviewId);
    }

}