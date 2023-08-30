package com.example.demo.dtos;

import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import com.example.demo.enums.CreditCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryDTO {
    private Order order;
    private BigDecimal paymentAmount;
    private Customer customer;
    private CreditCard creditCard;
}
