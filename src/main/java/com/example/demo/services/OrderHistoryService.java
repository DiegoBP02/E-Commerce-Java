package com.example.demo.services;

import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.OrderHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


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

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

}
