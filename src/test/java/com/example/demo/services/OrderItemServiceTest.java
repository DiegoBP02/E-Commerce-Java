package com.example.demo.services;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderItemDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.OrderItemRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import com.example.demo.utils.TestDataBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class OrderItemServiceTest extends ApplicationConfigTest {

    @Autowired
    private OrderItemService orderItemService;

    @MockBean
    private OrderItemRepository orderItemRepository;

    @MockBean
    private OrderService orderService;

    @MockBean
    private ProductService productService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Seller seller = (Seller) TestDataBuilder.buildUserWithId();
    private Product product = TestDataBuilder.buildProduct(seller);
    private Customer customer = TestDataBuilder.buildCustomer();
    private OrderItemDTO orderItemDTO = TestDataBuilder.buildOrderItemDTO();
    private Order order = TestDataBuilder.buildOrder(customer);
    private OrderItem orderItem = TestDataBuilder.buildOrderItem(order, product);

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
    void givenValidOrderItemDTOAndProductDoesNotExistsInOrder_whenCreate_thenReturnNewOrderItem() {
        when(orderService.findById(orderItemDTO.getOrderId()))
                .thenReturn(order);
        when(productService.findById(orderItemDTO.getProductId()))
                .thenReturn(product);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(orderItem);

        orderItemDTO.setQuantity(5);
        OrderItem expectedOrderItem = orderItem;
        expectedOrderItem.setQuantity(orderItemDTO.getQuantity());

        OrderItem result = orderItemService.create(orderItemDTO);

        assertEquals(orderItem, result);

        verifyAuthentication();
        verify(orderService, times(1)).findById(orderItemDTO.getOrderId());
        verify(productService, times(1)).findById(orderItemDTO.getProductId());
        verify(orderItemRepository, times(1)).save(expectedOrderItem);
    }

    @Test
    void givenValidOrderItemDTOAndProductAlreadyExistsInOrder_whenCreate_thenReturnUpdateOrderItemWithCorrectQuantity() {
        OrderItem orderItem1 = orderItem;
        product.setId(orderItemDTO.getProductId());
        orderItem1.setProduct(product);
        order.setItems(Collections.singletonList(orderItem1));

        when(orderService.findById(orderItemDTO.getOrderId()))
                .thenReturn(order);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenReturn(orderItem);

        orderItemDTO.setQuantity(5);
        OrderItem expectedOrderItem = orderItem;
        expectedOrderItem.setQuantity(orderItemDTO.getQuantity() + orderItem.getQuantity());

        OrderItem result = orderItemService.create(orderItemDTO);

        assertEquals(orderItem, result);

        verifyAuthentication();
        verify(orderService, times(1)).findById(orderItemDTO.getOrderId());
        verify(productService, never()).findById(any(UUID.class));
        verify(orderItemRepository, times(1)).save(expectedOrderItem);
    }

    @Test
    void givenNoOrder_whenCreate_thenThrowResourceNotFoundException() {
        when(orderService.findById(orderItemDTO.getOrderId()))
                .thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () ->
                orderItemService.create(orderItemDTO));

        verifyAuthentication();
        verify(orderService, times(1)).findById(orderItemDTO.getOrderId());
        verify(productService, never()).findById(any(UUID.class));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void givenOrderDoesNotBelongToUser_whenCreate_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());
        when(authentication.getPrincipal()).thenReturn(user2);
        when(orderService.findById(orderItemDTO.getOrderId()))
                .thenReturn(order);

        assertThrows(UnauthorizedAccessException.class, () ->
                orderItemService.create(orderItemDTO));

        verifyAuthentication();
        verify(orderService, times(1)).findById(orderItemDTO.getOrderId());
        verify(productService, never()).findById(any(UUID.class));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void givenOrderItems_whenFindAll_ThenReturnOrderItem() {
        List<OrderItem> orderItems = Collections.singletonList(orderItem);
        when(orderItemRepository.findAll()).thenReturn(orderItems);

        List<OrderItem> result = orderItemService.findAll();

        assertEquals(orderItems, result);

        verifyNoAuthentication();
        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    void givenOrderItems_whenFindByOrderId_ThenReturnOrderItem() {
        List<OrderItem> orderItems = Collections.singletonList(orderItem);
        when(orderItemRepository.findByOrderId(order.getId())).thenReturn(orderItems);

        List<OrderItem> result = orderItemService.findByOrderId(order.getId());

        assertEquals(orderItems, result);

        verifyNoAuthentication();
        verify(orderItemRepository, times(1)).findByOrderId(order.getId());
    }

    @Test
    void givenOrderItem_whenFindById_thenReturnOrderItem() {
        when(orderItemRepository.findById(orderItem.getId())).thenReturn(Optional.of(orderItem));

        OrderItem result = orderItemService.findById(orderItem.getId());

        assertEquals(orderItem, result);

        verifyNoAuthentication();
        verify(orderItemRepository, times(1)).findById(orderItem.getId());
    }

    @Test
    void givenNoOrderItem_whenFindById_thenThrowResourceNotFoundException() {
        when(orderItemRepository.findById(orderItem.getId())).thenThrow(ResourceNotFoundException.class);

        assertThrows(ResourceNotFoundException.class, () -> orderItemService.findById(orderItem.getId()));

        verifyNoAuthentication();
        verify(orderItemRepository, times(1)).findById(orderItem.getId());
    }

    @Test
    void givenValidIdAndOrderItemDTO_whenUpdate_thenReturnUpdatedOrderItem() {
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);

        orderItemDTO = OrderItemDTO.builder().quantity(5).build();

        OrderItem result = orderItemService.update(orderItem.getId(), orderItemDTO);

        assertEquals(orderItemDTO.getQuantity(), result.getQuantity());

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, times(1)).save(orderItem);
    }

    @Test
    void givenNoOrderItem_whenUpdate_thenThrowResourceNotFoundException() {
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);
        when(orderItemRepository.save(orderItem))
                .thenThrow(EntityNotFoundException.class);

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.update(orderItem.getId(), orderItemDTO));

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, times(1)).save(orderItem);
    }

    @Test
    void givenOrderItemDoesNotBelongToUser_whenUpdate_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);
        when(orderItemRepository.save(orderItem)).thenReturn(orderItem);

        assertThrows(UnauthorizedAccessException.class,
                () -> orderItemService.update(orderItem.getId(), orderItemDTO));

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, never()).save(orderItem);
    }

    @Test
    void givenOrderItem_whenDelete_thenDeleteOrderItem() {
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);

        orderItemService.delete(orderItem.getId());

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, times(1)).deleteById(orderItem.getId());
    }

    @Test
    void givenNoOrderItem_whenDelete_thenThrowResourceNotFoundException() {
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);
        doThrow(EmptyResultDataAccessException.class)
                .when(orderItemRepository).deleteById(orderItem.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> orderItemService.delete(orderItem.getId()));

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, times(1)).deleteById(orderItem.getId());
    }

    @Test
    void givenOrderItemAndDeleteCausesDataIntegrityViolationException_whenDelete_thenThrowDatabaseException() {
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);
        doThrow(DataIntegrityViolationException.class)
                .when(orderItemRepository).deleteById(orderItem.getId());

        assertThrows(DatabaseException.class,
                () -> orderItemService.delete(orderItem.getId()));

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, times(1)).deleteById(orderItem.getId());
    }

    @Test
    void givenOrderItemDoesNotBelongToUser_whenDelete_thenThrowUnauthorizedAccessException() {
        User user2 = mock(User.class);
        when(user2.getId()).thenReturn(UUID.randomUUID());

        when(authentication.getPrincipal()).thenReturn(user2);
        when(orderItemRepository.getReferenceById(orderItem.getId())).thenReturn(orderItem);

        assertThrows(UnauthorizedAccessException.class,
                () -> orderItemService.delete(orderItem.getId()));

        verifyAuthentication();
        verify(orderItemRepository, times(1)).getReferenceById(orderItem.getId());
        verify(orderItemRepository, never()).deleteById(orderItem.getId());
    }

}