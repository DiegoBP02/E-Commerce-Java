package com.example.demo.services.stripe;

import com.example.demo.dtos.StripeUserDTO;
import com.example.demo.entities.Order;
import com.example.demo.enums.CreditCard;
import com.example.demo.services.exceptions.StripeErrorException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static com.example.demo.services.stripe.StripeService.convertMoneyToCents;

@Service
public class StripePaymentService {

    private static final Logger logger = LoggerFactory.getLogger(StripePaymentService.class);

    public PaymentIntent createPaymentIntent(StripeUserDTO stripeUserDTO, Order order) {
        try {
            PaymentIntentCreateParams.AutomaticPaymentMethods automaticPaymentMethods
                    = PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setAllowRedirects(PaymentIntentCreateParams
                            .AutomaticPaymentMethods.AllowRedirects.NEVER)
                    .setEnabled(true)
                    .build();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setCustomer(stripeUserDTO.getId())
                    .setAmount(convertMoneyToCents(stripeUserDTO.getBalance()))
                    .setCurrency(stripeUserDTO.getCurrency())
                    .setAutomaticPaymentMethods(automaticPaymentMethods)
                    .setAmount(convertMoneyToCents(order.getTotalAmount()))
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            logger.info("A payment intent for user {} was created. PaymentIntentId: {}",
                    stripeUserDTO.getId(), paymentIntent.getId());

            return paymentIntent;
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

    public PaymentIntent createPaymentConfirmation(CreditCard creditCard, String paymentIntentId) {
        try {
            PaymentIntentConfirmParams params =
                    PaymentIntentConfirmParams.builder()
                            .setPaymentMethod(String.valueOf(creditCard))
                            .build();

            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent confirmedIntent = intent.confirm(params);

            logger.info("Payment confirmed. ConfirmedIntentId: {}, Status: {}, Amount: {}",
                    confirmedIntent.getId(), confirmedIntent.getStatus(), confirmedIntent.getAmount());

            return confirmedIntent;
        } catch (StripeException e) {
            throw new StripeErrorException(e.getStripeError().getMessage());
        }
    }

}
