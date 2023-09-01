package com.example.demo.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {
    @NotBlank
    @Size(min = 4, max = 30)
    private String oldPassword;
    @NotBlank
    @Size(min = 4, max = 30)
    private String newPassword;
}
