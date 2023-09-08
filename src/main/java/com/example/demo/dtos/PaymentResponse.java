package com.example.demo.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
public class PaymentResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    private Instant paymentCreatedAt;
    private BigDecimal amount;
    private BigDecimal endingBalance;

    @Builder
    public PaymentResponse(Instant createdAt, BigDecimal amount, BigDecimal endingBalance) {
        this.paymentCreatedAt = createdAt;
        this.amount = amount;
        this.endingBalance = endingBalance;
    }

}
