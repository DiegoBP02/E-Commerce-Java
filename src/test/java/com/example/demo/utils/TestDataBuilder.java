package com.example.demo.utils;

import com.example.demo.dtos.*;
import com.example.demo.entities.*;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.ProductCategory;
import com.example.demo.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestDataBuilder {
    private static final String defaultName = "name";
    private static final String defaultEmail = "email@email.com";
    private static final String defaultCustomerEmail = "customer@email.com";
    private static final String defaultPassword = "password";
    private static final Role defaultRole = Role.Seller;
    private static final String defaultProductName = "Sample Product";
    private static final String defaultProductDescription = "Sample product description";
    private static final BigDecimal defaultProductPrice = BigDecimal.ONE;
    private static final String defaultReviewComment = "Default review comment";
    private static final int defaultReviewRating = 4;
    private static final ProductCategory defaultProductCategory = ProductCategory.CAR_ACCESSORIES;
    private static final int defaultOrderItemQuantity = 1;
    private static final CreditCard defaultCreditCard = CreditCard.pm_card_visa;

    public static User buildUserNoId() {
        return Seller.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .build();
    }

    public static User buildUserWithId() {
        Seller seller = Seller.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .build();
        seller.setId(UUID.randomUUID());
        return seller;
    }

    public static Customer buildCustomerWithId() {
        Customer customer = Customer.builder()
                .name(defaultName)
                .email(defaultCustomerEmail)
                .password(defaultPassword)
                .build();
        customer.setId(UUID.randomUUID());
        return customer;
    }

    public static Customer buildCustomerNoId() {
        return Customer.builder()
                .name(defaultName)
                .email(defaultCustomerEmail)
                .password(defaultPassword)
                .build();
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

    public static Product buildProductWithId(Seller seller) {
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

    public static Review buildReviewWithId(Product product, Customer customer) {
        Review review = Review.builder()
                .product(product)
                .comment(defaultReviewComment)
                .rating(defaultReviewRating)
                .customer(customer)
                .build();
        review.setId(UUID.randomUUID());
        return review;
    }

    public static Review buildReviewNoId(Product product, Customer customer) {
        return Review.builder()
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

    public static Order buildOrderNoId() {
        return Order.builder()
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(Instant.now())
                .totalAmount(BigDecimal.ONE)
                .customer(buildCustomerWithId())
                .build();
    }

    public static Order buildOrderWithId() {
        return Order.builder()
                .id(UUID.randomUUID())
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(Instant.now())
                .totalAmount(BigDecimal.ONE)
                .customer(buildCustomerWithId())
                .build();
    }

    public static Order buildOrder(Customer customer) {
        return Order.builder()
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(Instant.now())
                .totalAmount(BigDecimal.ONE)
                .customer(customer)
                .build();
    }

    public static OrderItem buildOrderItemNoId(Order order, Product product) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(defaultOrderItemQuantity)
                .build();
    }

    public static OrderItem buildOrderItemWithId() {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(buildOrderWithId())
                .product(buildProductWithId((Seller) buildUserWithId()))
                .quantity(defaultOrderItemQuantity)
                .build();
    }

    public static OrderItem buildOrderItemWithId(Order order, Product product) {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .product(product)
                .quantity(defaultOrderItemQuantity)
                .build();
    }

    public static OrderItemDTO buildOrderItemDTO() {
        return OrderItemDTO.builder()
                .quantity(defaultOrderItemQuantity)
                .productId(UUID.randomUUID())
                .build();
    }

    public static OrderHistory buildOrderHistory(Order order) {
        return OrderHistory.builder()
                .order(order)
                .customer(order.getCustomer())
                .creditCard(defaultCreditCard)
                .paymentAmount(defaultProductPrice)
                .build();
    }

    public static OrderHistory buildOrderHistoryWithId(Order order) {
        OrderHistory orderHistory = OrderHistory.builder()
                .order(order)
                .customer(order.getCustomer())
                .creditCard(defaultCreditCard)
                .paymentAmount(defaultProductPrice)
                .build();
        orderHistory.setId(UUID.randomUUID());
        return orderHistory;
    }

    public static OrderHistoryDTO buildOrderHistoryDTO(Order order) {
        return OrderHistoryDTO.builder()
                .order(order)
                .customer(order.getCustomer())
                .creditCard(defaultCreditCard)
                .paymentAmount(defaultProductPrice)
                .build();
    }

    public static OrderPaymentDTO buildOrderPaymentDTO() {
        return OrderPaymentDTO.builder()
                .creditCard(CreditCard.pm_card_visa)
                .build();
    }

    public static ChangePasswordDTO buildChangePasswordDTO() {
        return ChangePasswordDTO.builder()
                .newPassword("newPassword")
                .oldPassword(defaultPassword)
                .build();
    }

    public static ForgotPasswordDTO buildForgotPasswordDTO() {
        return ForgotPasswordDTO.builder()
                .email(defaultEmail)
                .build();
    }

    public static ResetPasswordDTO buildResetPasswordDTO() {
        return ResetPasswordDTO.builder()
                .password(defaultPassword)
                .build();
    }

    public static <T> Page<T> buildPage(T entity, Integer pageNo, Integer pageSize, String sortBy) {
        return new PageImpl<>(Collections.singletonList(entity), PageRequest.of(pageNo, pageSize, Sort.by(sortBy)), 1);
    }

    public static <T> List<T> buildList(T entity) {
        return Collections.singletonList(entity);
    }

    public static UserLoginResponseDTO buildUserLoginResponseDTO(RegisterDTO registerDTO) {
        return UserLoginResponseDTO.builder()
                .token("token")
                .email(registerDTO.getEmail())
                .name(registerDTO.getName())
                .role(registerDTO.getRole())
                .build();
    }
}
