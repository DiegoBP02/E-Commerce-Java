package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.ReviewDTO;
import com.example.demo.dtos.UpdateReviewDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.services.ReviewService;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

class ReviewControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/reviews";

    @MockBean
    private ReviewService reviewService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Customer customer = TestDataBuilder.buildCustomer();
    private Product product = TestDataBuilder.buildProduct(seller);
    private Review review = TestDataBuilder.buildReview(product, customer);
    private ReviewDTO reviewDTO = TestDataBuilder.buildReviewDTO();
    private ReviewDTO invalidReviewDTO = mock(ReviewDTO.class);
    private UpdateReviewDTO updateReviewDTO = TestDataBuilder.buildUpdateReviewDTO();

    private MockHttpServletRequestBuilder mockPostRequest
            (Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    private MockHttpServletRequestBuilder mockGetRequest() {
        return MockMvcRequestBuilders
                .get(PATH)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder mockGetRequest(String endpoint) {
        return MockMvcRequestBuilders
                .get(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder mockPathRequest
            (String endpoint, Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .patch(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    private MockHttpServletRequestBuilder mockDeleteRequest(String endpoint) {
        return MockMvcRequestBuilders
                .delete(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

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
    @WithMockUser(authorities = "random")
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
    void givenReviewsAndNoUser_whenFindAll_thenReturnReviewPage() throws Exception {
        Page<Review> reviewPage = mock(PageImpl.class);

        when(reviewService.findAll(0, 5, "rating")).thenReturn(reviewPage);

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(reviewPage)));

        verify(reviewService, times(1)).findAll(0, 5, "rating");
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
    @WithMockUser(authorities = "Customer")
    void givenValidBodyAndUser_whenUpdate_thenReturnReview() throws Exception {
        when(reviewService.update(review.getId(), updateReviewDTO)).thenReturn(review);

        mockMvc.perform(mockPathRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(review)));

        verify(reviewService, times(1)).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoReview_whenUpdate_thenHandleResourceNotFoundException() throws Exception {
        when(reviewService.update(review.getId(), updateReviewDTO))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockPathRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(reviewService, times(1)).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenUpdate_thenHandleMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(mockPathRequest(review.getId().toString(), invalidReviewDTO))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(reviewService, never()).update(review.getId(), updateReviewDTO);
    }

    @Test
    void givenNoUser_whenUpdate_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockPathRequest(review.getId().toString(), updateReviewDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(reviewService, never()).update(review.getId(), updateReviewDTO);
    }

    @Test
    @WithMockUser(authorities = "random")
    void givenInvalidUserAuthority_whenUpdate_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockPathRequest(review.getId().toString(), updateReviewDTO))
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
    @WithMockUser(authorities = "random")
    void givenInvalidUserAuthority_whenDelete_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest(review.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(reviewService, never()).delete(review.getId());
    }

}