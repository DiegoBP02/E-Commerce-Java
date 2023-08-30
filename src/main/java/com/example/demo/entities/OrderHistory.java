package com.example.demo.entities;

import com.example.demo.entities.user.Customer;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class OrderHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @JsonIgnore
    @ToString.Exclude
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private CreditCard creditCard;

    @Column(nullable = false)
    private BigDecimal paymentAmount;

    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Builder
    public OrderHistory(Order order, Customer customer, CreditCard creditCard, BigDecimal paymentAmount) {
        this.order = order;
        this.customer = customer;
        this.paymentDate = LocalDateTime.now();
        this.creditCard = creditCard;
        this.paymentAmount = paymentAmount;
        this.orderStatus = OrderStatus.Delivered;
    }

}