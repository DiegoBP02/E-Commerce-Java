package com.example.demo.dtos;

import com.example.demo.enums.CreditCard;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentDTO {
    @NotNull
    private CreditCard creditCard;
}