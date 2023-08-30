package com.example.demo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private LocalDateTime paymentCreatedAt;
    private BigDecimal amount;
    private BigDecimal endingBalance;

    @Builder
    public PaymentResponse(long createdAt, BigDecimal amount, BigDecimal endingBalance) {
        this.paymentCreatedAt = formatDate(createdAt);
        this.amount = amount;
        this.endingBalance = endingBalance;
    }

    private LocalDateTime formatDate(long unixDate) {
        Instant instant = Instant.ofEpochSecond(unixDate);
        return instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

}
