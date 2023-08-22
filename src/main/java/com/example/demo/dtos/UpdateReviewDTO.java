package com.example.demo.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReviewDTO {
    @NotBlank
    @Size(min = 3, max = 150)
    private String comment;

    @NotNull
    @Min(0)
    @Max(5)
    private int rating;

}
