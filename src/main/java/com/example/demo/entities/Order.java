package com.example.demo.entities;

import com.example.demo.entities.user.Customer;
import com.example.demo.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant orderDate;

    @Transient
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder
    public Order(Customer customer) {
        this.customer = customer;
        this.orderDate = Instant.now();
        this.status = OrderStatus.Active;
        this.items = new ArrayList<>();
    }

    public BigDecimal getTotalAmount() {
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        for (OrderItem orderItem : items) {
            calculatedTotal = calculatedTotal.add(orderItem.getItemTotal());
        }
        return calculatedTotal;
    }

}