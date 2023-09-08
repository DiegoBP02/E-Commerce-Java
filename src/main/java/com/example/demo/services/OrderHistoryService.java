package com.example.demo.services;

import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.OrderHistoryRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUser;
import static com.example.demo.services.utils.CheckOwnership.checkOwnership;


@Service
public class OrderHistoryService {

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    public OrderHistory create(OrderHistoryDTO orderHistoryDTO) {
        Customer customer = (Customer) getCurrentUser();
        OrderHistory orderHistory = OrderHistory.builder()
                .order(orderHistoryDTO.getOrder())
                .paymentAmount(orderHistoryDTO.getPaymentAmount())
                .customer(customer)
                .creditCard(orderHistoryDTO.getCreditCard())
                .build();

        return orderHistoryRepository.save(orderHistory);
    }

    public OrderHistory findById(UUID id) {
        OrderHistory orderHistory = orderHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(id));
        User user = getCurrentUser();
        checkOwnership(user, orderHistory.getCustomer().getId());
        return orderHistory;
    }

    public Page<OrderHistory> findByCurrentUser(Integer pageNo, Integer pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Customer customer = (Customer) getCurrentUser();

        return orderHistoryRepository.findAllByCustomer(customer, paging);
    }

}
