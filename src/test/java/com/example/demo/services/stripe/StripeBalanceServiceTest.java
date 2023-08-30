package com.example.demo.services.stripe;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.services.exceptions.StripeErrorException;
import com.example.demo.services.stripe.utils.StripeUtils;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerBalanceTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StripeBalanceServiceTest extends ApplicationConfigTest {

    @Autowired
    private StripeBalanceService stripeBalanceService;

    @Autowired
    private StripeUtils stripeUtils;

    private Customer customer;

    @BeforeEach
    void setupStripeCustomer(){
        customer = stripeUtils.setupStripeCustomer();
    }

    @AfterEach
    void deleteStripeCustomer() {
        StripeUtils.deleteStripeCustomer(customer);
    }

    @Test
    void givenCustomerAndAmount_whenCreateBalanceTransfer_thenReturnCustomerBalanceTransaction() {
        CustomerBalanceTransaction result = stripeBalanceService.createBalanceTransfer(customer.getId(), 1);
        assertNotNull(result);
    }

}