package com.example.demo.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    @NotNull
    private UUID productId;

    @NotBlank
    @Size(min = 3, max = 150)
    private String comment;

    @NotNull
    @Min(0)
    @Max(5)
    private int rating;

}
