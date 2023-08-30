package com.example.demo.dtos;

import com.example.demo.enums.CreditCard;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeUserDTO {
    private CreditCard creditCard;
    private BigDecimal balance;
    private String id;
    private String currency;
}
