package com.example.demo.services;

import com.example.demo.dtos.OrderItemDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.OrderItemRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;
import static com.example.demo.services.utils.CheckOwnership.checkOwnership;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    public OrderItem create(OrderItemDTO orderItemDTO) {
        User user = getCurrentUser();
        Order order = orderService.findActiveOrderByCurrentUser();
        checkOwnership(user, order.getCustomer().getId());

        OrderItem existingOrderItem = findExistingItem(order, orderItemDTO.getProductId());

        if (existingOrderItem != null) {
            OrderItemDTO updateData = OrderItemDTO.builder()
                    .quantity(existingOrderItem.getQuantity() + orderItemDTO.getQuantity())
                    .build();
            updateData(existingOrderItem, updateData);

            return orderItemRepository.save(existingOrderItem);
        } else {
            Product product = productService.findById(orderItemDTO.getProductId());
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .quantity(orderItemDTO.getQuantity())
                    .product(product)
                    .build();

            return orderItemRepository.save(orderItem);
        }
    }

    private OrderItem findExistingItem(Order order, UUID productId) {
        for (OrderItem orderItem : order.getItems()) {
            if (orderItem.getProduct().getId().equals(productId)) {
                return orderItem;
            }
        }
        return null;
    }

    public List<OrderItem> findAll() {
        return orderItemRepository.findAll();
    }

    public List<OrderItem> findByOrderId(UUID orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public OrderItem findById(UUID id) {
        return orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public OrderItem update(UUID id, OrderItemDTO obj) {
        try {
            OrderItem entity = orderItemRepository.getReferenceById(id);
            User user = getCurrentUser();
            checkOwnership(user, entity.getOrder().getCustomer().getId());
            updateData(entity, obj);

            return orderItemRepository.save(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException(id);
        }
    }

    private void updateData(OrderItem entity, OrderItemDTO obj) {
        entity.setQuantity(obj.getQuantity());
    }

    public void delete(UUID id) {
        try {
            OrderItem entity = orderItemRepository.getReferenceById(id);

            User user = getCurrentUser();
            checkOwnership(user, entity.getOrder().getCustomer().getId());

            orderItemRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

}
