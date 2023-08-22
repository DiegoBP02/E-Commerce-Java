package com.example.demo.services;

import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.enums.OrderStatus;
import com.example.demo.exceptions.UniqueConstraintViolationError;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.services.exceptions.DatabaseException;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.demo.services.utils.CheckOwnership.checkOwnership;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public Order create() {
        try {
            Customer user = (Customer) getCurrentUser();
            Order order = Order.builder()
                    .orderDate(LocalDateTime.now())
                    .customer(user)
                    .status(OrderStatus.Pending)
                    .items(new ArrayList<>())
                    .build();
            return orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            throw new UniqueConstraintViolationError();
        }
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    public Order findByCurrentUser() {
        User user = getCurrentUser();
        return orderRepository.findByCustomerId(user.getId())
                .orElseThrow(ResourceNotFoundException::new);
    }

    private Order findByUser(User user) {
        return orderRepository.findByCustomerId(user.getId())
                .orElseThrow(ResourceNotFoundException::new);
    }

    public void delete(UUID id) {
        try {
            Order entity = orderRepository.getReferenceById(id);

            User user = getCurrentUser();
            checkOwnership(user, entity.getCustomer().getId());

            orderRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new ResourceNotFoundException(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    public void deleteByCurrentUser() {
        try {
            User user = getCurrentUser();
            Order order = findByUser(user);

            orderRepository.deleteById(order.getId());
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException(e.getMessage());
        }
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
