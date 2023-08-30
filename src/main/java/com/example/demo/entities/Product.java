package com.example.demo.entities;

import com.example.demo.entities.user.Seller;
import com.example.demo.enums.ProductCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "products")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductCategory category;

}