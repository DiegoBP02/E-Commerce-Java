package com.example.demo.entities.user;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.Review;
import com.example.demo.entities.exceptions.NoActiveOrderException;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Customer extends User {

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Order> orders;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderHistory> orderHistory = new ArrayList<>();

    @Builder
    public Customer(String name, String email, String password, Role role) {
        super(name, email, password, role);
    }

    @JsonIgnore
    public Order getActiveOrder() {
        for (Order order : orders) {
            if (order.getStatus().equals(OrderStatus.Active)) {
                return order;
            }
        }
        throw new NoActiveOrderException("No active order found");
    }
}
