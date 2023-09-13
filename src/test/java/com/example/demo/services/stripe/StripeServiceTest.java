package com.example.demo.services.stripe;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.dtos.StripeUserDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import com.example.demo.enums.CreditCard;
import com.example.demo.enums.Currency;
import com.example.demo.enums.OrderStatus;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.services.OrderService;
import com.example.demo.services.exceptions.InvalidPaymentStatusException;
import com.example.demo.utils.TestDataBuilder;
import com.stripe.model.CustomerBalanceTransaction;
import com.stripe.model.PaymentIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


class StripeServiceTest extends ApplicationConfigTest {

    @Autowired
    private StripeService stripeService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private StripeUserService stripeUserService;

    @MockBean
    private StripePaymentService stripePaymentService;

    @MockBean
    private StripeBalanceService stripeBalanceService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Customer customer = TestDataBuilder.buildCustomerWithId();
    private Order order = TestDataBuilder.buildOrder(customer);
    private com.stripe.model.Customer mockCustomerStripe = mock(com.stripe.model.Customer.class);
    private PaymentIntent mockPaymentIntent = mock(PaymentIntent.class);
    private PaymentIntent mockPaymentConfirmation = mock(PaymentIntent.class);
    private CustomerBalanceTransaction mockCustomerBalanceTransaction = mock(CustomerBalanceTransaction.class);
    private StripeUserDTO stripeUserDTO;
    private OrderPaymentDTO orderPaymentDTO = TestDataBuilder.buildOrderPaymentDTO();
    private CreditCard creditCard = orderPaymentDTO.getCreditCard();

    private void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeEach
    void setupTestData() {
        order.setStatus(OrderStatus.Active);
        customer.setOrders(Collections.singletonList(order));

        when(mockCustomerStripe.getId()).thenReturn("1");
        when(mockCustomerStripe.getBalance()).thenReturn(1000L);
        when(mockCustomerStripe.getCurrency()).thenReturn(Currency.usd.toString());

        stripeUserDTO = StripeUserDTO.builder()
                .id(mockCustomerStripe.getId())
                .balance(BigDecimal.valueOf(mockCustomerStripe.getBalance()))
                .currency(mockCustomerStripe.getCurrency())
                .build();

        when(mockPaymentIntent.getId()).thenReturn("1");

        when(mockPaymentConfirmation.getStatus()).thenReturn(PaymentStatus.succeeded.toString());
        when(mockPaymentConfirmation.getCustomer()).thenReturn("1");
        when(mockPaymentConfirmation.getAmount()).thenReturn(1000L);

        when(mockCustomerBalanceTransaction.getAmount()).thenReturn(1000L);
        when(mockCustomerBalanceTransaction.getEndingBalance()).thenReturn(1000L);
    }

    @Test
    void givenValidCreditCard_whenCreateOrderPayment_thenReturnPaymentResponse() {
        when(stripeUserService.getStripeUser(creditCard)).thenReturn(mockCustomerStripe);

        when(stripePaymentService.createPaymentIntent(stripeUserDTO, customer.getActiveOrder()))
                .thenReturn(mockPaymentIntent);
        when(stripePaymentService.createPaymentConfirmation(creditCard, mockPaymentIntent.getId()))
                .thenReturn(mockPaymentConfirmation);

        when(stripeBalanceService.createBalanceTransfer
                (mockPaymentConfirmation.getCustomer(), mockPaymentConfirmation.getAmount()))
                .thenReturn(mockCustomerBalanceTransaction);

        PaymentResponse expectedResult = PaymentResponse.builder()
                .createdAt(Instant.ofEpochSecond(mockCustomerBalanceTransaction.getCreated()))
                .amount(BigDecimal.valueOf(mockCustomerBalanceTransaction.getAmount() / 100.0))
                .endingBalance(BigDecimal.valueOf(mockCustomerBalanceTransaction.getEndingBalance() / 100.0))
                .build();

        PaymentResponse result = stripeService.createOrderPayment(orderPaymentDTO);

        assertEquals(expectedResult, result);

        verifyAuthentication();
        verify(stripeUserService, times(1)).getStripeUser(creditCard);
        verify(orderService, times(1)).checkUserOrder();
        verify(stripePaymentService, times(1))
                .createPaymentIntent(stripeUserDTO, customer.getActiveOrder());
        verify(stripePaymentService, times(1))
                .createPaymentConfirmation(creditCard, mockPaymentIntent.getId());
        verify(stripeBalanceService, times(1))
                .createBalanceTransfer
                        (mockPaymentConfirmation.getCustomer(), mockPaymentConfirmation.getAmount());
        verify(orderService, times(1)).moveOrderToHistory
                (creditCard, BigDecimal.valueOf(mockCustomerBalanceTransaction.getAmount() / 100.0));
    }

    @Test
    void givenInvalidPaymentStatus_whenCreateOrderPayment_thenThrowInvalidPaymentStatusException() {
        when(mockPaymentConfirmation.getStatus()).thenReturn("random");

        when(stripeUserService.getStripeUser(creditCard)).thenReturn(mockCustomerStripe);

        when(stripePaymentService.createPaymentIntent(stripeUserDTO, customer.getActiveOrder()))
                .thenReturn(mockPaymentIntent);
        when(stripePaymentService.createPaymentConfirmation(creditCard, mockPaymentIntent.getId()))
                .thenReturn(mockPaymentConfirmation);

        when(stripeBalanceService.createBalanceTransfer
                (mockPaymentConfirmation.getCustomer(), mockPaymentConfirmation.getAmount()))
                .thenReturn(mockCustomerBalanceTransaction);

        assertThrows(InvalidPaymentStatusException.class,
                () -> stripeService.createOrderPayment(orderPaymentDTO)
        );

        verifyAuthentication();
        verify(stripeUserService, times(1)).getStripeUser(creditCard);
        verify(orderService, times(1)).checkUserOrder();
        verify(stripePaymentService, times(1))
                .createPaymentIntent(stripeUserDTO, customer.getActiveOrder());
        verify(stripePaymentService, times(1))
                .createPaymentConfirmation(creditCard, mockPaymentIntent.getId());
        verify(stripeBalanceService, never())
                .createBalanceTransfer
                        (mockPaymentConfirmation.getCustomer(), mockPaymentConfirmation.getAmount());
        verify(orderService, never()).moveOrderToHistory
                (creditCard, BigDecimal.valueOf(mockCustomerBalanceTransaction.getAmount() / 100.0));
    }

    @Test
    void givenMoney_whenConvertMoneyToCents_thenConvertsCorrectly() {
        long result = StripeService.convertMoneyToCents(BigDecimal.ONE);
        assertEquals(100L, result);
    }

    @Test
    void givenCents_whenConvertCentosToMoney_thenConvertsCorrectly() {
        BigDecimal result = StripeService.convertCentsToMoney(100L);
        assertEquals(BigDecimal.valueOf(1.0), result);
    }

}
