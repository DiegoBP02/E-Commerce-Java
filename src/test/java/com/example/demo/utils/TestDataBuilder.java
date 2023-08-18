package com.example.demo.utils;

import com.example.demo.dtos.*;
import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.ProductCategory;
import com.example.demo.enums.Role;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

public class TestDataBuilder {
    private static final String defaultName = "name";
    private static final String defaultEmail = "email@email.com";
    private static final String defaultPassword = "email@email.com";
    private static final Role defaultRole = Role.Seller;
    private static final String defaultProductName = "Sample Product";
    private static final String defaultProductDescription = "Sample product description";
    private static final BigDecimal defaultProductPrice = BigDecimal.valueOf(99.99);
    private static final String defaultReviewComment = "Default review comment";
    private static final int defaultReviewRating = 4;
    private static final ProductCategory defaultProductCategory =  ProductCategory.CAR_ACCESSORIES;

    public static User buildUser() {
        return Seller.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(defaultRole)
                .build();
    }

    public static User buildUserWithId() {
        Seller seller = Seller.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(defaultRole)
                .build();
        seller.setId(UUID.randomUUID());
        return seller;
    }

    public static Customer buildCustomer() {
        Customer customer = Customer.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(Role.Customer)
                .build();
        customer.setId(UUID.randomUUID());
        return customer;
    }

    public static RegisterDTO buildRegisterDTO() {
        return RegisterDTO.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(defaultRole)
                .build();
    }

    public static LoginDTO buildLoginDTO() {
        return LoginDTO.builder()
                .email(defaultEmail)
                .password(defaultPassword)
                .build();
    }

    public static Product buildProduct(Seller seller) {
        return Product.builder()
                .id(UUID.randomUUID())
                .name(defaultProductName)
                .description(defaultProductDescription)
                .price(defaultProductPrice)
                .seller(seller)
                .reviews(new ArrayList<>())
                .category(defaultProductCategory)
                .build();
    }

    public static Product buildProductNoId(Seller seller) {
        return Product.builder()
                .name(defaultProductName)
                .description(defaultProductDescription)
                .price(defaultProductPrice)
                .seller(seller)
                .reviews(new ArrayList<>())
                .category(defaultProductCategory)
                .build();
    }

    public static ProductDTO buildProductDTO() {
        return ProductDTO.builder()
                .name(defaultName)
                .description(defaultProductDescription)
                .price(defaultProductPrice)
                .category(defaultProductCategory)
                .build();
    }

    public static Review buildReview(Product product, Customer customer) {
        return Review.builder()
                .id(UUID.randomUUID())
                .product(product)
                .comment(defaultReviewComment)
                .rating(defaultReviewRating)
                .customer(customer)
                .build();
    }

    public static ReviewDTO buildReviewDTO() {
        return ReviewDTO.builder()
                .productId(UUID.randomUUID())
                .comment(defaultReviewComment)
                .rating(defaultReviewRating)
                .build();
    }

    public static UpdateReviewDTO buildUpdateReviewDTO() {
        return UpdateReviewDTO.builder()
                .comment("new comment")
                .rating(1)
                .build();
    }

}
