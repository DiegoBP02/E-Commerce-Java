package com.example.demo.services.stripe;

import com.example.demo.enums.CreditCard;
import com.example.demo.services.exceptions.StripeErrorException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerCollection;
import com.stripe.param.CustomerCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.example.demo.services.stripe.StripeService.convertMoneyToCents;
import static com.example.demo.services.stripe.StripeService.getCurrentCustomer;

@Service
public class StripeUserService {

    private static final Logger logger = LoggerFactory.getLogger(StripeUserService.class);

    @Value("${stripe.secret.key}")
    private String secretKey;

    public Customer getStripeUser(CreditCard creditCard) {
        com.example.demo.entities.user.Customer user = getCurrentCustomer();

        Customer customerAlreadyExists = findStripeCustomerByEmail(user.getEmail());
        if (customerAlreadyExists != null) {
            return customerAlreadyExists;
        }

        return createUser(creditCard);
    }

    private Customer createUser(CreditCard creditCard) {
        try {
            com.example.demo.entities.user.Customer user = getCurrentCustomer();

            StripeClient client = new StripeClient(secretKey);
            CustomerCreateParams params =
                    CustomerCreateParams
                            .builder()
                            .setEmail(user.getEmail())
                            .setPaymentMethod(String.valueOf(creditCard))
                            .setBalance(convertMoneyToCents(generateRandomValue()))
                            .build();

            Customer customer = client.customers().create(params);

            logger.info("Stripe user created - id: {}, email: {}, currency: {}",
                    customer.getId(), customer.getEmail(), customer.getCurrency());

            return customer;
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

    private Customer findStripeCustomerByEmail(String email) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("email", email);

            CustomerCollection customers = Customer.list(params);

            if (customers.getData().isEmpty()) {
                logger.info("No Stripe user found for email: {}", email);
                return null;
            }

            Customer customer = customers.getData().get(0);
            logger.info("Stripe user found - id: {}, email: {}, currency: {}",
                    customer.getId(), customer.getEmail(), customer.getCurrency());

            return customer;
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

    private BigDecimal generateRandomValue() {
        return BigDecimal.valueOf(Math.random() * (5000 /*max*/ - 2000 /*min*/) + 2000);
    }
}
