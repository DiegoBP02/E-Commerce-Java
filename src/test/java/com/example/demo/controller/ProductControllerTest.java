package com.example.demo.controller;

import com.example.demo.dtos.ProductDTO;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.services.ProductService;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest extends ApplicationConfigTestController {

    private static final String PATH = "/products";

    public ProductControllerTest() {
        super(PATH);
    }

    @MockBean
    private ProductService productService;

    private User user = TestDataBuilder.buildUserWithId();
    private Product product = TestDataBuilder.buildProductWithId((Seller) user);
    private ProductDTO productDTO = TestDataBuilder.buildProductDTO();
    private ProductDTO invalidProductDTO = mock(ProductDTO.class);
    Page<Product> productPage = mock(PageImpl.class);

    @Test
    @WithMockUser(authorities = "Seller")
    void givenValidBody_whenCreate_thenReturnProductAndCreated() throws Exception {
        when(productService.create(productDTO)).thenReturn(product);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(productDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(product)));

        verify(productService, times(1)).create(productDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidBody_whenCreate_thenHandleMethodArgumentNotValidException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(invalidProductDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(productService, never()).create(invalidProductDTO);
    }

    @Test
    void givenNoUser_whenCreate_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(productDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(productService, never()).create(productDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthority_whenCreate_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(productDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(productService, never()).create(productDTO);
    }

    @Test
    void givenProductsAndNoUser_whenFindAll_thenReturnProductPage() throws Exception {
        Page<Product> productPage = mock(PageImpl.class);

        when(productService.findAll(0, 5, Sort.Direction.ASC, "name"))
                .thenReturn(productPage);

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(productPage)));

        verify(productService, times(1))
                .findAll(0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    void givenProductsAndNoUser_whenFindByCategory_thenReturnProduct() throws Exception {
        when(productService.findByCategory(product.getCategory(),
                0, 5, Sort.Direction.ASC, "name")).thenReturn(productPage);

        MockHttpServletRequestBuilder mockRequest = mockGetRequestWithParams
                ("/category", "productCategory", product.getCategory().toString());

        mockMvc.perform(mockRequest)
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(productPage)));

        verify(productService, times(1))
                .findByCategory(product.getCategory(), 0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    void givenMissingParamAndNoUser_whenFindByCategory_thenHandleMissingServletRequestParameterException() throws Exception {
        when(productService.findByCategory(product.getCategory(),
                0, 5, Sort.Direction.ASC, "name")).thenReturn(productPage);

        mockMvc.perform(mockGetRequest("/category"))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MissingServletRequestParameterException));

        verifyNoInteractions(productService);
    }

    @Test
    void givenInvalidParamAndNoUser_whenFindByCategory_thenHandleMethodArgumentTypeMismatchException() throws Exception {
        when(productService.findByCategory(product.getCategory(),
                0, 5, Sort.Direction.ASC, "name")).thenReturn(productPage);

        MockHttpServletRequestBuilder mockRequest = mockGetRequestWithParams
                ("/category", "productCategory", "random");

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentTypeMismatchException));

        verify(productService, never()).findByCategory
                (product.getCategory(),0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    void givenProductAndNoUser_whenFindById_thenReturnProduct() throws Exception {
        when(productService.findById(product.getId())).thenReturn(product);

        mockMvc.perform(mockGetRequest(product.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(product)));

        verify(productService, times(1)).findById(product.getId());
    }

    @Test
    void givenNoProductAndNoUser_whenFindById_thenHandleResourceNotFoundException() throws Exception {
        when(productService.findById(product.getId()))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest(product.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(productService, times(1)).findById(product.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenProduct_whenFindByCurrentUser_thenReturnProductPage() throws Exception {
        when(productService.findByCurrentUser(0, 5, Sort.Direction.ASC, "name"))
                .thenReturn(productPage);

        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(productPage)));

        verify(productService, times(1))
                .findByCurrentUser(0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    void givenNoUser_whenFindByCurrentUser_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthority_whenFindByCurrentUser_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest("user"))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verifyNoInteractions(productService);
    }

    @Test
    void givenProduct_whenFindBySellerId_thenReturnProductPage() throws Exception {
        when(productService.findAllBySeller(user.getId(), 0, 5, Sort.Direction.ASC, "name"))
                .thenReturn(productPage);

        mockMvc.perform(mockGetRequest("seller/" + user.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(productPage)));

        verify(productService, times(1))
                .findAllBySeller(user.getId(), 0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    void givenInvalidCasting_whenFindBySellerId_thenHandleClassCastException() throws Exception {
        when(productService.findAllBySeller(user.getId(), 0, 5, Sort.Direction.ASC, "name"))
                .thenThrow(ClassCastException.class);

        mockMvc.perform(mockGetRequest("seller/" + user.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ClassCastException));

        verify(productService, times(1))
                .findAllBySeller(user.getId(), 0, 5, Sort.Direction.ASC, "name");
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenValidBodyAndUser_whenUpdate_thenReturnProduct() throws Exception {
        when(productService.update(product.getId(), productDTO)).thenReturn(product);

        mockMvc.perform(mockPatchRequest(product.getId().toString(), productDTO))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(product)));

        verify(productService, times(1)).update(product.getId(), productDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenNoProduct_whenUpdate_thenHandleResourceNotFoundException() throws Exception {
        when(productService.update(product.getId(), productDTO))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockPatchRequest(product.getId().toString(), productDTO))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(productService, times(1)).update(product.getId(), productDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidBody_whenUpdate_thenHandleMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(mockPatchRequest(product.getId().toString(), invalidProductDTO))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(productService, never()).update(product.getId(), invalidProductDTO);
    }

    @Test
    void givenNoUser_whenUpdate_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockPatchRequest(product.getId().toString(), productDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(productService, never()).update(product.getId(), productDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthority_whenUpdate_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockPatchRequest(product.getId().toString(), productDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(productService, never()).update(product.getId(), productDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenValidUserAndProduct_whenDelete_thenReturnNoContent() throws Exception {
        mockMvc.perform(mockDeleteRequest(product.getId().toString()))
                .andExpect(status().isNoContent());

        verify(productService, times(1)).delete(product.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenProductDoesNotBelongToUser_whenDelete_thenHandleUnauthorizedAccessException() throws Exception {
        doThrow(UnauthorizedAccessException.class)
                .when(productService).delete(product.getId());

        mockMvc.perform(mockDeleteRequest(product.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(productService, times(1)).delete(product.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenDatabaseError_whenDelete_thenHandleDatabaseException() throws Exception {
        doThrow(DatabaseException.class)
                .when(productService).delete(product.getId());

        mockMvc.perform(mockDeleteRequest(product.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof DatabaseException));

        verify(productService, times(1)).delete(product.getId());
    }

    @Test
    void givenNoUser_whenDelete_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockDeleteRequest(product.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(productService, never()).delete(product.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidUserAuthority_whenDelete_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest(product.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(productService, never()).delete(product.getId());
    }

}