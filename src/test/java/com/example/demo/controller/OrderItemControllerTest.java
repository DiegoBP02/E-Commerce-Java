package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderItemDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.services.OrderItemService;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderItemControllerTest extends ApplicationConfigTest {

    private static final String PATH = "/orderItems";

    @MockBean
    private OrderItemService orderItemService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private OrderItemDTO orderItemDTO = TestDataBuilder.buildOrderItemDTO();
    private OrderItem orderItem = TestDataBuilder.buildOrderItemWithId();
    private OrderItemDTO invalidOrderItemDTO = mock(OrderItemDTO.class);
    private List<OrderItem> orderItems = TestDataBuilder.buildList(orderItem);
    private Order order = TestDataBuilder.buildOrderWithId();

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

    private MockHttpServletRequestBuilder mockPatchRequest
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
    void givenValidBody_whenCreate_thenReturnOrderItemAndCreated() throws Exception {
        when(orderItemService.create(orderItemDTO)).thenReturn(orderItem);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(orderItem)));

        verify(orderItemService, times(1)).create(orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidOrder_whenCreate_thenHandleResourceNotFoundException() throws Exception {
        when(orderItemService.create(orderItemDTO)).thenThrow(ResourceNotFoundException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderItemService, times(1)).create(orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderDoesNotBelongToUser_whenCreate_thenHandleUnauthorizedAccessException() throws Exception {
        when(orderItemService.create(orderItemDTO)).thenThrow(UnauthorizedAccessException.class);

        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(orderItemService, times(1)).create(orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenCreate_thenHandleMethodArgumentNotValidException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(invalidOrderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(orderItemService, never()).create(invalidOrderItemDTO);
    }

    @Test
    void givenNoUser_whenCreate_thenReturnStatus403Forbidden() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).create(orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenCreate_thenHandleAccessDeniedException() throws Exception {
        MockHttpServletRequestBuilder mockRequest = mockPostRequest(orderItemDTO);

        mockMvc.perform(mockRequest)
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).create(orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Admin")
    void givenOrderItemsAndUserIsAdmin_whenFindAll_thenReturnOrderItems() throws Exception {
        when(orderItemService.findAll()).thenReturn(orderItems);

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderItems)));

        verify(orderItemService, times(1)).findAll();
    }

    @Test
    void givenNoUser_whenFindAll_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindAll_thenHandleAccessDeniedException() throws Exception {

        mockMvc.perform(mockGetRequest())
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).findAll();
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderItemsAndValidUser_whenFindByOrderId_thenReturnOrderItem() throws Exception {
        when(orderItemService.findByOrderId(order.getId())).thenReturn(orderItems);

        mockMvc.perform(mockGetRequest("/order/" + order.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderItems)));

        verify(orderItemService, times(1)).findByOrderId(order.getId());
    }

    @Test
    void givenNoUser_whenFindByOrderId_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest("/order/" + order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).findByOrderId(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindByOrderId_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest("/order/" + order.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).findByOrderId(order.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderItemAndValidUser_whenFindById_thenReturnOrderItem() throws Exception {
        when(orderItemService.findById(orderItem.getId())).thenReturn(orderItem);

        mockMvc.perform(mockGetRequest(orderItem.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderItem)));

        verify(orderItemService, times(1)).findById(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoOrderItemAndNoUser_whenFindById_thenHandleResourceNotFoundException() throws Exception {
        when(orderItemService.findById(orderItem.getId()))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockGetRequest(orderItem.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderItemService, times(1)).findById(orderItem.getId());
    }

    @Test
    void givenNoUser_whenFindById_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockGetRequest(orderItem.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).findById(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenFindById_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockGetRequest(orderItem.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).findById(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidBodyAndUser_whenUpdate_thenReturnOrderItem() throws Exception {
        when(orderItemService.update(orderItem.getId(), orderItemDTO)).thenReturn(orderItem);

        mockMvc.perform(mockPatchRequest(orderItem.getId().toString(), orderItemDTO))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(orderItem)));

        verify(orderItemService, times(1)).update(orderItem.getId(), orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenNoOrderItem_whenUpdate_thenHandleResourceNotFoundException() throws Exception {
        when(orderItemService.update(orderItem.getId(), orderItemDTO))
                .thenThrow(ResourceNotFoundException.class);

        mockMvc.perform(mockPatchRequest(orderItem.getId().toString(), orderItemDTO))
                .andExpect(status().isNotFound())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof ResourceNotFoundException));

        verify(orderItemService, times(1)).update(orderItem.getId(), orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenInvalidBody_whenUpdate_thenHandleMethodArgumentNotValidException() throws Exception {
        mockMvc.perform(mockPatchRequest(orderItem.getId().toString(), invalidOrderItemDTO))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof MethodArgumentNotValidException));

        verify(orderItemService, never()).update(orderItem.getId(), invalidOrderItemDTO);
    }

    @Test
    void givenNoUser_whenUpdate_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockPatchRequest(orderItem.getId().toString(), orderItemDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).update(orderItem.getId(), orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenUpdate_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockPatchRequest(orderItem.getId().toString(), orderItemDTO))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).update(orderItem.getId(), orderItemDTO);
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenValidUserAndOrderItem_whenDelete_thenReturnNoContent() throws Exception {
        mockMvc.perform(mockDeleteRequest(orderItem.getId().toString()))
                .andExpect(status().isNoContent());

        verify(orderItemService, times(1)).delete(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenOrderItemDoesNotBelongToUser_whenDelete_thenHandleUnauthorizedAccessException() throws Exception {
        doThrow(UnauthorizedAccessException.class)
                .when(orderItemService).delete(orderItem.getId());

        mockMvc.perform(mockDeleteRequest(orderItem.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof UnauthorizedAccessException));

        verify(orderItemService, times(1)).delete(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Customer")
    void givenDatabaseError_whenDelete_thenHandleDatabaseException() throws Exception {
        doThrow(DatabaseException.class)
                .when(orderItemService).delete(orderItem.getId());

        mockMvc.perform(mockDeleteRequest(orderItem.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof DatabaseException));

        verify(orderItemService, times(1)).delete(orderItem.getId());
    }

    @Test
    void givenNoUser_whenDelete_thenReturnStatus403Forbidden() throws Exception {
        mockMvc.perform(mockDeleteRequest(orderItem.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertEquals("Access Denied",
                                result.getResponse().getErrorMessage()));

        verify(orderItemService, never()).delete(orderItem.getId());
    }

    @Test
    @WithMockUser(authorities = "Seller")
    void givenInvalidUserAuthority_whenDelete_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(mockDeleteRequest(orderItem.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(result ->
                        assertTrue(result.getResolvedException()
                                instanceof AccessDeniedException));

        verify(orderItemService, never()).delete(orderItem.getId());
    }

}