package com.example.demo.repositories;

import com.example.demo.entities.Product;
import com.example.demo.entities.user.Seller;
import com.example.demo.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCategory(ProductCategory productCategory);

    Page<Product> findAllBySeller(Seller seller, Pageable paging);
}
