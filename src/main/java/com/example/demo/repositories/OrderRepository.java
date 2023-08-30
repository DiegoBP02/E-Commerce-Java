package com.example.demo.repositories;

import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByCustomerId(UUID id);

    @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.status = com.example.demo.enums.OrderStatus.Active")
    Optional<Order> findActiveOrderByCurrentUser(@Param("customer") Customer customer);
}
