package com.example.demo.controller;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.dtos.ForgotPasswordDTO;
import com.example.demo.dtos.ResetPasswordDTO;
import com.example.demo.services.PasswordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/password")
public class PasswordController {

    @Autowired
    private PasswordService passwordService;

    @PostMapping(value = "/change-password")
    public ResponseEntity<Void> changePassword
            (@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        passwordService.changePassword(changePasswordDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/forgot-password")
    public ResponseEntity<String> forgotPassword
            (HttpServletRequest request, @RequestBody @Valid ForgotPasswordDTO forgotPasswordDTO) {
        passwordService.forgotPassword(request, forgotPasswordDTO);

        return ResponseEntity.ok()
                .body("We have sent a reset password link to your email. Please check.");
    }

    @PostMapping(value = "/reset-password")
    public ResponseEntity<String> resetPassword
            (@RequestParam UUID token, @RequestBody @Valid ResetPasswordDTO resetPasswordDTO) throws Exception {
        passwordService.resetPassword(token, resetPasswordDTO);

        return ResponseEntity.ok().body("You have successfully changed your password.");
    }

}
