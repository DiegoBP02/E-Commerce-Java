package com.example.demo.controller;

import com.example.demo.dtos.ProductDTO;
import com.example.demo.entities.Product;
import com.example.demo.enums.ProductCategory;
import com.example.demo.services.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PreAuthorize("hasAuthority('Seller')")
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        Product product = productService.create(productDTO);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.getId()).toUri();

        return ResponseEntity.created(uri).body(product);
    }

    @GetMapping
    public ResponseEntity<Page<Product>> findAll(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Sort.Direction sortOrder = Sort.Direction.fromString(sortDirection);
        Page<Product> productPage = productService.findAll(pageNo, pageSize, sortOrder, sortBy);
        return ResponseEntity.ok().body(productPage);
    }

    @GetMapping(value = "/category")
    public ResponseEntity<List<Product>> findByCategory(@RequestParam ProductCategory productCategory) {
        return ResponseEntity.ok().body(productService.findByCategory(productCategory));
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Product> findById(@PathVariable UUID id) {
        return ResponseEntity.ok().body(productService.findById(id));
    }

    @PreAuthorize("hasAuthority('Seller')")
    @GetMapping(value = "/user")
    public ResponseEntity<Page<Product>> findByCurrentUser(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Sort.Direction sortOrder = Sort.Direction.fromString(sortDirection);
        Page<Product> productPage = productService.findByCurrentUser(pageNo, pageSize, sortOrder, sortBy);
        return ResponseEntity.ok().body(productPage);
    }

    @GetMapping(value = "/seller/{sellerId}")
    public ResponseEntity<Page<Product>> findBySellerId(
            @PathVariable UUID sellerId,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "5") Integer pageSize,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(defaultValue = "name") String sortBy
    ) {
        Sort.Direction sortOrder = Sort.Direction.fromString(sortDirection);
        Page<Product> productPage =
                productService.findAllBySeller(sellerId, pageNo, pageSize, sortOrder, sortBy);
        return ResponseEntity.ok().body(productPage);
    }

    @PreAuthorize("hasAuthority('Seller')")
    @PatchMapping(value = "/{id}")
    public ResponseEntity<Product> update(@PathVariable UUID id,
                                          @Valid @RequestBody ProductDTO obj) {
        return ResponseEntity.ok().body(productService.update(id, obj));
    }

    @PreAuthorize("hasAnyAuthority('Seller', 'Admin')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
