package com.example.demo.controller;

import com.example.demo.entities.Order;
import com.example.demo.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PreAuthorize("hasAuthority('Customer')")
    @PostMapping
    public ResponseEntity<Order> create() {
        Order order = orderService.create();

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.getId()).toUri();

        return ResponseEntity.created(uri).body(order);
    }

    @PreAuthorize("hasAuthority('Admin')")
    @GetMapping
    public ResponseEntity<List<Order>> findAll() {
        List<Order> orders = orderService.findAll();
        return ResponseEntity.ok().body(orders);
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/{id}")
    public ResponseEntity<Order> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(orderService.findById(id));
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/user")
    public ResponseEntity<Order> findActiveOrderByCurrentUser() {
        return ResponseEntity.ok().body(orderService.findActiveOrderByCurrentUser());
    }

    @PreAuthorize("hasAuthority('Customer')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('Customer')")
    @DeleteMapping(value = "/user")
    public ResponseEntity<Void> deleteByCurrentUser() {
        orderService.deleteByCurrentUser();
        return ResponseEntity.noContent().build();
    }
}
