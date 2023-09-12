package com.example.demo.controller;

import com.example.demo.dtos.OrderPaymentDTO;
import com.example.demo.dtos.PaymentResponse;
import com.example.demo.services.stripe.StripeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/payment")
public class PaymentController {

    @Autowired
    private StripeService stripeService;

    @PostMapping
    @PreAuthorize("hasAuthority('Customer')")
    public ResponseEntity<PaymentResponse> createOrderPayment(@RequestBody @Valid OrderPaymentDTO orderPaymentDTO) {
        PaymentResponse paymentIntent = stripeService.createOrderPayment(orderPaymentDTO);
        return ResponseEntity.ok(paymentIntent);
    }

}
