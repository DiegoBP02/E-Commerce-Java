package com.example.demo.dtos;

import com.example.demo.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResponseDTO {
    @NotBlank
    private String token;
    @NotBlank
    @Size(min = 3, max = 40)
    private String name;
    @NotBlank
    @Email
    private String email;
    @NotNull
    private Role role;
}
