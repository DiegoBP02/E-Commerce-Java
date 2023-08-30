package com.example.demo.services.stripe;

import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.dtos.StripeUserDTO;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.services.OrderService;
import com.example.demo.services.exceptions.InvalidPaymentStatusException;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.CustomerBalanceTransaction;
import com.stripe.model.PaymentIntent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StripeUserService stripeUserService;

    @Autowired
    private StripePaymentService stripePaymentService;

    @Autowired
    private StripeBalanceService stripeBalanceService;

    @PostConstruct
    private void init() {
        Stripe.apiKey = secretKey;
    }

    public PaymentResponse createOrderPayment(OrderPaymentDTO orderPaymentDTO) {
        com.example.demo.entities.user.Customer user = getCurrentCustomer();
        Customer customer = stripeUserService.getStripeUser(orderPaymentDTO.getCreditCard());
        StripeUserDTO stripeUserDTO = StripeUserDTO.builder()
                .id(customer.getId())
                .balance(BigDecimal.valueOf(customer.getBalance()))
                .currency(customer.getCurrency())
                .build();

        orderService.checkUserOrder();

        PaymentIntent paymentIntent = stripePaymentService.createPaymentIntent(stripeUserDTO, user.getActiveOrder());
        PaymentIntent paymentConfirmation = stripePaymentService.createPaymentConfirmation(orderPaymentDTO.getCreditCard(), paymentIntent.getId());
        checkPaymentStatus(paymentConfirmation);

        CustomerBalanceTransaction customerBalanceTransaction
                = stripeBalanceService.createBalanceTransfer(paymentConfirmation.getCustomer(),
                paymentConfirmation.getAmount());
        orderService.moveOrderToHistory(orderPaymentDTO.getCreditCard(), convertCentsToMoney(paymentConfirmation.getAmount()));

        return PaymentResponse.builder()
                .createdAt(customerBalanceTransaction.getCreated())
                .amount(convertCentsToMoney(paymentConfirmation.getAmount()))
                .endingBalance(convertCentsToMoney(customerBalanceTransaction.getEndingBalance()))
                .build();
    }

    private void checkPaymentStatus(PaymentIntent paymentConfirmation) {
        if (!paymentConfirmation.getStatus().equals(PaymentStatus.succeeded.toString())) {
            throw new InvalidPaymentStatusException
                    ("Something went wrong during payment confirmation!");
        }
    }

    public static long convertMoneyToCents(BigDecimal money) {
        return money.longValue() * 100;
    }

    public static BigDecimal convertCentsToMoney(long cents) {
        return BigDecimal.valueOf(cents / 100.00);
    }

    public static com.example.demo.entities.user.Customer getCurrentCustomer() {
        return (com.example.demo.entities.user.Customer)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}