package com.example.demo.controller;

import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.services.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping(value = "/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterDTO registerDTO) {
        String token = authenticationService.register(registerDTO);
        return ResponseEntity.ok().body(token);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String token = authenticationService.login(loginDTO);
        return ResponseEntity.ok().body(token);
    }

    @GetMapping(value = "/confirmation-request")
    @PreAuthorize("hasAnyAuthority('Customer', 'Seller')")
    public ResponseEntity<String> confirmationRequest(HttpServletRequest request) {
        authenticationService.confirmationRequest(request);
        String message = "We have sent a confirmation account link to your email. Please check.";
        return ResponseEntity.ok().body(message);
    }

    @PostMapping(value = "/confirm-account")
    public ResponseEntity<String> confirmAccount(HttpServletRequest request, @RequestParam UUID token) throws Exception {
        authenticationService.confirmAccount(request,token);

        return ResponseEntity.ok().body("You have successfully confirmed your account.");
    }

}