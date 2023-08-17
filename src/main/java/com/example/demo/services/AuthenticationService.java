package com.example.demo.services;


import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.user.Admin;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;
import com.example.demo.exceptions.UniqueConstraintViolationError;
import com.example.demo.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found: " + email));
    }

    public String register(RegisterDTO registerDTO) {
        try {
            User user = createUserBasedOnRole(registerDTO);
            userRepository.save(user);
            return tokenService.generateToken(user);
        } catch (DataIntegrityViolationException e) {
            throw new UniqueConstraintViolationError("user", "email");
        }
    }

    public String login(LoginDTO loginDTO) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword());

        Authentication authentication = this.authenticationManager.authenticate
                (usernamePasswordAuthenticationToken);

        User user = (User) authentication.getPrincipal();

        return tokenService.generateToken(user);
    }

    private User createUserBasedOnRole(RegisterDTO registerDTO) {
        Role role = registerDTO.getRole();
        return switch (role) {
            case Customer -> Customer.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordEncoder.encode(registerDTO.getPassword()))
                    .role(Role.Customer)
                    .build();
            case Seller -> Seller.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordEncoder.encode(registerDTO.getPassword()))
                    .role(Role.Seller)
                    .build();
            case Admin -> Admin.builder()
                    .name(registerDTO.getName())
                    .email(registerDTO.getEmail())
                    .password(passwordEncoder.encode(registerDTO.getPassword()))
                    .role(Role.Admin)
                    .build();
        };
    }

}
