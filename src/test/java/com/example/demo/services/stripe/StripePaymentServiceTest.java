package com.example.demo.services.stripe;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.StripeUserDTO;
import com.example.demo.entities.Order;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.Currency;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.services.stripe.utils.StripeUtils;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StripePaymentServiceTest extends ApplicationConfigTest {

    @Autowired
    private StripePaymentService stripePaymentService;

    @Autowired
    private StripeUtils stripeUtils;

    private Customer customer;
    private Order order = mock(Order.class);

    @BeforeEach
    void setupStripeCustomer() {
        customer = stripeUtils.setupStripeCustomer();
    }

    @AfterEach
    void deleteStripeCustomer() {
        StripeUtils.deleteStripeCustomer(customer);
    }

    @BeforeEach
    void setupTestData() {
        when(order.getTotalAmount()).thenReturn(BigDecimal.ONE);
    }

    @Test
    void givenStripeUserDTOAndOrder_whenCreatePaymentIntent_thenReturnPaymentIntent() {
        StripeUserDTO stripeUserDTO = StripeUserDTO.builder()
                .id(customer.getId())
                .balance(BigDecimal.ONE)
                .currency(Currency.usd.toString())
                .creditCard(CreditCard.pm_card_visa)
                .build();
        PaymentIntent result = stripePaymentService.createPaymentIntent(stripeUserDTO, order);

        assertEquals(stripeUserDTO.getId(), result.getCustomer());
        assertEquals(stripeUserDTO.getCurrency(), result.getCurrency());
    }

    @Test
    void givenCreditCardAndPaymentIntent_whenCreatePaymentConfirmation_thenReturnSucceededPaymentIntent() {
        StripeUserDTO stripeUserDTO = StripeUserDTO.builder()
                .id(customer.getId())
                .balance(BigDecimal.ONE)
                .currency(Currency.usd.toString())
                .creditCard(CreditCard.pm_card_visa)
                .build();
        PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(stripeUserDTO, order);

        PaymentIntent result = stripePaymentService
                .createPaymentConfirmation(CreditCard.pm_card_visa, paymentIntent.getId());

        assertNotNull(result);
        assertEquals(paymentIntent.getCustomer(), result.getCustomer());
        assertEquals(PaymentStatus.succeeded.toString(), result.getStatus());
    }

}