package com.example.demo.services.stripe;

import com.example.demo.services.exceptions.InsufficientBalanceException;
import com.example.demo.services.exceptions.StripeErrorException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.CustomerBalanceTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.example.demo.services.stripe.StripeService.convertCentsToMoney;

@Service
public class StripeBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(StripeBalanceService.class);

    public CustomerBalanceTransaction createBalanceTransfer(String customerId, long amount) {
        try {
            Customer customer = Customer.retrieve(customerId);

            if (customer.getBalance() < amount) {
                throw new InsufficientBalanceException
                        ("You don't have enough money to make this payment. " +
                                "Amount required: " + convertCentsToMoney(amount) + ". " +
                                "Your balance: " + convertCentsToMoney(customer.getBalance()));
            }

            Map<String, Object> params = new HashMap<>();
            params.put("amount", -amount);
            params.put("currency", customer.getCurrency());

            CustomerBalanceTransaction customerBalanceTransaction
                    = customer.balanceTransactions().create(params);

            logger.info("Customer balance transaction successful: Amount transferred: {}," +
                            "Currency: {}, Customer ending balance: {}",
                    customerBalanceTransaction.getAmount(),
                    customerBalanceTransaction.getCurrency(),
                    customerBalanceTransaction.getEndingBalance());

            return customerBalanceTransaction;
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

}
