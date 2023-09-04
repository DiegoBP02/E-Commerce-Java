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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
                .email(defaultCustomerEmail)
                .password(defaultPassword)
                .role(Role.Customer)
                .build();
        customer.setId(UUID.randomUUID());
        return customer;
    }

    public static Customer buildCustomerNoId() {
        return Customer.builder()
                .name(defaultName)
                .email(defaultCustomerEmail)
                .password(defaultPassword)
                .role(Role.Customer)
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

    public static Order buildOrder() {
        return Order.builder()
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ONE)
                .customer(buildCustomer())
                .build();
    }

    public static Order buildOrderWithId() {
        return Order.builder()
                .id(UUID.randomUUID())
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ONE)
                .customer(buildCustomer())
                .build();
    }

    public static Order buildOrder(Customer customer) {
        return Order.builder()
                .items(new ArrayList<>())
                .status(OrderStatus.Pending)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ONE)
                .customer(customer)
                .build();
    }

    public static OrderItem buildOrderItem(Order order, Product product) {
        return OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(defaultOrderItemQuantity)
                .build();
    }

    public static OrderItem buildOrderItemWithId() {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(buildOrder())
                .product(buildProduct((Seller) buildUser()))
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
                .orderId(UUID.randomUUID())
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
}
