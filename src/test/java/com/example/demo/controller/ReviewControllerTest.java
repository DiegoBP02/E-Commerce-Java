package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.services.ReviewService;
import com.example.demo.services.exceptions.*;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest extends ApplicationConfigTestController {

    private static final String PATH = "/reviews";

    public ReviewControllerTest() {
        super(PATH);
    }

    @MockBean
    private ReviewService reviewService;
    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Customer customer = TestDataBuilder.buildCustomerWithId();
    private Product product = TestDataBuilder.buildProductWithId(seller);
    private Review review = TestDataBuilder.buildReviewWithId(product, customer);
    private ReviewDTO reviewDTO = TestDataBuilder.buildReviewDTO();
    private ReviewDTO invalidReviewDTO = mock(ReviewDTO.class);
    private UpdateReviewDTO updateReviewDTO = TestDataBuilder.buildUpdateReviewDTO();

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidBody_whenCreate_thenReturnReviewAndCreated() throws Exception {
        when(reviewService.create(reviewDTO)).thenReturn(review);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(reviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(review)));

        verify(reviewService, times(1)).create(reviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenCreate_thenHandleMethodArgumentNotValidException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(invalidReviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(reviewService, never()).create(invalidReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenUserDidNotPurchaseProduct_whenCreate_thenHandleProductNotPurchasedException() throws Exception {
        when(reviewService.create(reviewDTO)).thenThrow(ProductNotPurchasedException.class);
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(reviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ProductNotPurchasedException));

        verify(reviewService, times(1)).create(reviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenReviewForProductAlreadyExists_whenCreate_thenHandleUniqueConstraintViolationError() throws Exception {
        when(reviewService.create(reviewDTO)).thenThrow(UniqueConstraintViolationError.class);
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(reviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UniqueConstraintViolationError));

        verify(reviewService, times(1)).create(reviewDTO);
    }

    @Test
    void givenNoUser_whenCreate_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(reviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(reviewService, never()).create(reviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenCreate_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(reviewDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(reviewService, never()).create(reviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void givenReviewsAndAdmin_whenFindAll_thenReturnReviewPage() throws Exception {
        Page<Review> reviewPage = mock(PageImpl.class);

        when(reviewService.findAll(0, 5, Sort.Direction.ASC, "rating"))
                .thenReturn(reviewPage);

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(reviewPage)));

        verify(reviewService, times(1))
                .findAll(0, 5, Sort.Direction.ASC, "rating");
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthoritySeller_whenFindAll_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(reviewService);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthorityCustomer_whenFindAll_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(reviewService);
    }

    @Test
    void givenReviewAndNoUser_whenFindById_thenReturnReview() throws Exception {
        when(reviewService.findById(review.getId())).thenReturn(review);

        mockMvc.perform(mockGetRequest(review.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(review)));

        verify(reviewService, times(1)).findById(review.getId());
    }

    @Test
    void givenNoReviewAndNoUser_whenFindById_thenHandleResourceNotFoundException() throws Exception {
        when(reviewService.findById(review.getId()))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest(review.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(reviewService, times(1)).findById(review.getId());
    }

    @Test
    void givenReview_whenFindAllByProduct_thenReturnReviewPage() throws Exception {
        Page<Review> reviewPage = mock(PageImpl.class);

        when(reviewService
                .findAllByProduct(product.getId(), 0, 5, Sort.Direction.ASC, "rating"))
                .thenReturn(reviewPage);

        mockMvc.perform(mockGetRequest("product/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(reviewPage)));

        verify(reviewService, times(1))
                .findAllByProduct(product.getId(), 0, 5, Sort.Direction.ASC, "rating");
    }


    @Test
    @WithMockUser(authorities = "Customer")
    void givenReview_whenFindByCurrentUser_thenReturnReviewPage() throws Exception {
        Page<Review> reviewPage = mock(PageImpl.class);

        when(reviewService.findByCurrentUser(0, 5, Sort.Direction.ASC, "rating"))
                .thenReturn(reviewPage);

        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(reviewPage)));

        verify(reviewService, times(1))
                .findByCurrentUser(0, 5, Sort.Direction.ASC, "rating");
    }

    @Test
    void givenNoUser_whenFindByCurrentUser_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(reviewService);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindByCurrentUser_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(reviewService);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidBodyAndUser_whenUpdate_thenReturnReview() throws Exception {
        when(reviewService.update(review.getId(), updateReviewDTO)).thenReturn(review);

        mockMvc.perform(mockPatchRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(review)));

        verify(reviewService, times(1)).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoReview_whenUpdate_thenHandleResourceNotFoundException() throws Exception {
        when(reviewService.update(review.getId(), updateReviewDTO))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockPatchRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(reviewService, times(1)).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenUpdate_thenHandleMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(mockPatchRequest(review.getId().toString(), invalidReviewDTO))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(reviewService, never()).update(review.getId(), updateReviewDTO);
    }

    @Test
    void givenNoUser_whenUpdate_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockPatchRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(reviewService, never()).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenUpdate_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockPatchRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(reviewService, never()).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidUserAndReview_whenDelete_thenReturnNoContent() throws Exception {
        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isNoContent());

        verify(reviewService, times(1)).delete(review.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenReviewDoesNotBelongToUser_whenDelete_thenHandleUnauthorizedAccessException() throws Exception {
        doThrow(UnauthorizedAccessException.class)
                .when(reviewService).delete(review.getId());

        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(reviewService, times(1)).delete(review.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenDatabaseError_whenDelete_thenHandleDatabaseException() throws Exception {
        doThrow(DatabaseException.class)
                .when(reviewService).delete(review.getId());

        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof DatabaseException));

        verify(reviewService, times(1)).delete(review.getId());
    }

    @Test
    void givenNoUser_whenDelete_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(reviewService, never()).delete(review.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenDelete_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(reviewService, never()).delete(review.getId());
    }

}