package com.example.demo.controller;

import com.example.demo.dtos.ChangePasswordDTO;
import com.example.demo.services.PasswordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
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
}
