package com.example.demo.repositories;

import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, UUID> {
    List<OrderHistory> findAllByCustomer(Customer customer);

    Page<OrderHistory> findAllByCustomer(Customer customer, Pageable pageable);
}
