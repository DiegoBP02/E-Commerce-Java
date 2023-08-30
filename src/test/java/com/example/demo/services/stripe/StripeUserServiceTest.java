package com.example.demo.services.stripe;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.entities.user.Customer;
import com.example.demo.enums.CreditCard;
import com.example.demo.services.exceptions.InvalidPaymentStatusException;
import com.example.demo.services.exceptions.StripeErrorException;
import com.example.demo.services.stripe.utils.StripeUtils;
import com.example.demo.utils.TestDataBuilder;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.CustomerCreateParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;

import static com.example.demo.services.stripe.StripeService.convertMoneyToCents;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class StripeUserServiceTest extends ApplicationConfigTest {

    @Autowired
    private StripeUserService stripeUserService;

    private Authentication authentication;
    private SecurityContext securityContext;

    private Customer customer = TestDataBuilder.buildCustomer();

    @BeforeEach
    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customer);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void givenCreditCard_whenGetStripeUser_thenReturnCustomer() {
        com.stripe.model.Customer result = stripeUserService.getStripeUser(CreditCard.pm_card_visa);

        assertNotNull(result);
        assertEquals(customer.getEmail(),result.getEmail());

        verify(authentication, atLeastOnce()).getPrincipal();
        verify(securityContext, atLeastOnce()).getAuthentication();
        verify(authentication, atMost(2)).getPrincipal();
        verify(securityContext, atMost(2)).getAuthentication();

        StripeUtils.deleteStripeCustomer(result);
    }

}