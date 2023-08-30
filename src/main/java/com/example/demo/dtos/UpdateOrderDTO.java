package com.example.demo.dtos;

import com.example.demo.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderDTO {
    private OrderStatus orderStatus;
}
