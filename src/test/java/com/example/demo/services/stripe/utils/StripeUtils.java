package com.example.demo.services.stripe.utils;

import com.example.demo.enums.CreditCard;
import com.example.demo.services.exceptions.StripeErrorException;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Component
public class StripeUtils {

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct
    private void init() {
        Stripe.apiKey = secretKey;
    }

    public static void deleteStripeCustomer(Customer customer) {
        Customer deletedCustomer = null;
        try {
            deletedCustomer = customer.delete();
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }

        assertEquals(customer.getId(), deletedCustomer.getId());
        assertTrue(deletedCustomer.getDeleted());
    }

    public Customer setupStripeCustomer() {
        try {
            StripeClient client = new StripeClient(secretKey);
            CustomerCreateParams params =
                    CustomerCreateParams
                            .builder()
                            .setPaymentMethod(String.valueOf(CreditCard.pm_card_visa))
                            .setBalance(1000L * 1000)
                            .build();
            return client.customers().create(params);
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }
}
