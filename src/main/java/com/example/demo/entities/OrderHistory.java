package com.example.demo.entities;

import com.example.demo.entities.user.Customer;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant paymentDate;

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
        this.paymentDate = Instant.now();
        this.creditCard = creditCard;
        this.paymentAmount = paymentAmount;
        this.orderStatus = OrderStatus.Delivered;
    }

}