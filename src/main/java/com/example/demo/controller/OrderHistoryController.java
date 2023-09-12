package com.example.demo.controller;

import com.example.demo.entities.OrderHistory;
import com.example.demo.services.OrderHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/orderHistory")
public class OrderHistoryController {

    @Autowired
    private OrderHistoryService orderHistoryService;

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/{id}")
    public ResponseEntity<OrderHistory> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(orderHistoryService.findById(id));
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/user")
    public ResponseEntity<Page<OrderHistory>> findByCurrentUser(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "paymentDate") String sortBy
    ) {
        Sort.Direction sortOrder = Sort.Direction.fromString(sortDirection);
        Page<OrderHistory> orderHistoryPage =
                orderHistoryService.findByCurrentUser(pageNo, pageSize, sortOrder, sortBy);
        return ResponseEntity.ok().body(orderHistoryPage);
    }
}
