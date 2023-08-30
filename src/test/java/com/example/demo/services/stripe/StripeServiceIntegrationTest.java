package com.example.demo.services.stripe;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderHistoryDTO;
import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderHistory;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.OrderStatus;
import com.example.demo.repositories.OrderRepository;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.OrderHistoryService;
import com.example.demo.services.exceptions.StripeErrorException;
import com.example.demo.services.stripe.utils.StripeUtils;
import com.example.demo.utils.TestDataBuilder;
import com.stripe.exception.StripeException;
import com.stripe.model.CustomerCollection;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StripeServiceIntegrationTest extends ApplicationConfigTest {

    @Autowired
    private StripeService stripeService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private OrderHistoryService orderHistoryService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Customer customer = TestDataBuilder.buildCustomer();
    private Order order = TestDataBuilder.buildOrder(customer);
    private Seller seller = (Seller) TestDataBuilder.buildUser();
    private Product product = TestDataBuilder.buildProduct(seller);
    private OrderItem orderItem = TestDataBuilder.buildOrderItem(order, product);
    private OrderHistory orderHistory = TestDataBuilder.buildOrderHistory(order);
    private OrderHistoryDTO orderHistoryDTO = TestDataBuilder.buildOrderHistoryDTO(order);
    private OrderPaymentDTO orderPaymentDTO = TestDataBuilder.buildOrderPaymentDTO();

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setupTestData(){
        order.setStatus(OrderStatus.Active);
        order.setItems(Collections.singletonList(orderItem));
        customer.setOrders(Collections.singletonList(order));
    }

    @AfterEach
    void deleteStripeCustomer(){
        Map<String, Object> params = new HashMap<>();
        params.put("email", customer.getEmail());
        try {
            CustomerCollection customers = com.stripe.model.Customer.list(params);
            com.stripe.model.Customer customer = customers.getData().get(0);
            StripeUtils.deleteStripeCustomer(customer);
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

    @Test
    void givenCreditCard_whenCreateOrderPayment_thenReturnPaymentResponse() {
        when(orderRepository.findActiveOrderByCurrentUser(customer))
                .thenReturn(Optional.of(order));
        when(userRepository.save(customer)).thenReturn(customer);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderHistoryService.create(orderHistoryDTO)).thenReturn(orderHistory);

        PaymentResponse result = stripeService.createOrderPayment(orderPaymentDTO);
        assertNotNull(result);
    }
}