package com.example.demo.controller;

import com.example.demo.dtos.OrderItemDTO;
import com.example.demo.entities.OrderItem;
import com.example.demo.services.OrderItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping(value = "/orderItems")
public class OrderItemController {

    @Autowired
    private OrderItemService orderItemService;

    @PreAuthorize("hasAuthority('Customer')")
    @PostMapping
    public ResponseEntity<OrderItem> create(@Valid @RequestBody OrderItemDTO orderItemDTO) {
        OrderItem orderItem = orderItemService.create(orderItemDTO);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(orderItem.getId()).toUri();

        return ResponseEntity.created(uri).body(orderItem);
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping
    public ResponseEntity<List<OrderItem>> findAll() {
        List<OrderItem> orderItems = orderItemService.findAll();
        return ResponseEntity.ok().body(orderItems);
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/order/{id}")
    public ResponseEntity<List<OrderItem>> findByOrderId(@PathVariable UUID id) {
        List<OrderItem> orderItems = orderItemService.findByOrderId(id);
        return ResponseEntity.ok().body(orderItems);
    }

    @PreAuthorize("hasAuthority('Customer')")
    @GetMapping(value = "/{id}")
    public ResponseEntity<OrderItem> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(orderItemService.findById(id));
    }

    @PreAuthorize("hasAuthority('Customer')")
    @PatchMapping(value = "/{id}")
    public ResponseEntity<OrderItem> update(@PathVariable UUID id,
                                            @Valid @RequestBody OrderItemDTO obj) {
        return ResponseEntity.ok().body(orderItemService.update(id, obj));
    }

    @PreAuthorize("hasAuthority('Customer')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        orderItemService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
