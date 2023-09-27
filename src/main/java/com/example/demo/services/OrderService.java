package com.example.demo.services;

import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;
import static com.example.demo.services.utils.CheckOwnership.checkOwnership;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderHistoryService orderHistoryService;

    @Transactional
    public Order create() {
        try {
            Customer user = (Customer) getCurrentUser();

            Optional<Order> existingActiveOrder = orderRepository.findActiveOrderByCurrentUser(user);

            if (existingActiveOrder.isPresent()) {
                throw new ActiveOrderAlreadyExistsException("Active order already exists!");
            }

            Order order = Order.builder()
                    .customer(user)
                    .build();
            return orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            throw new UniqueConstraintViolationError();
        }
    }

    public Page<Order> findAll(Integer pageNo, Integer pageSize, Sort.Direction sortOrder, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, sortOrder, sortBy);

        return orderRepository.findAll(paging);
    }

    public Order findById(UUID id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        checkOwnership(user, order.getCustomer().getId());
        return order;
    }

    public Order findActiveOrderByCurrentUser() {
        Customer user = (Customer) getCurrentUser();

        return orderRepository.findActiveOrderByCurrentUser(user)
                .orElseThrow(ResourceNotFoundException::new);
    }

    public Order findOrCreateActiveOrderByCurrentUser() {
        Customer user = (Customer) getCurrentUser();

        return orderRepository.findActiveOrderByCurrentUser(user)
                .orElseGet(() -> create());
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

    public void checkUserOrder() {
        Order order = findActiveOrderByCurrentUser();

        if (order.getItems().isEmpty()) {
            throw new InvalidOrderException("Invalid order: The order does not contain any items");
        }
    }

    public void moveOrderToHistory(CreditCard creditCard, BigDecimal paymentAmount) {
        Customer user = (Customer) getCurrentUser();
        Order order = findActiveOrderByCurrentUser();

        OrderHistoryDTO orderHistoryDTO = OrderHistoryDTO.builder()
                .order(order)
                .creditCard(creditCard)
                .paymentAmount(paymentAmount)
                .build();
        OrderHistory orderHistory = orderHistoryService.create(orderHistoryDTO);

        user.getOrderHistory().add(orderHistory);
        userRepository.save(user);
        order.setStatus(OrderStatus.Delivered);
        orderRepository.save(order);
    }

}
