package com.example.demo.utils;

import com.example.demo.dtos.LoginDTO;
import com.example.demo.dtos.RegisterDTO;
import com.example.demo.entities.user.Seller;
import com.example.demo.entities.user.User;
import com.example.demo.enums.Role;

public class TestDataBuilder {
    private static final String defaultName = "name";
    private static final String defaultEmail = "email@email.com";
    private static final String defaultPassword = "email@email.com";
    private static final Role defaultRole = Role.Seller;

    public static User buildUser() {
        return Seller.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(defaultRole)
                .build();
    }

    public static RegisterDTO buildRegisterDTO() {
        return RegisterDTO.builder()
                .name(defaultName)
                .email(defaultEmail)
                .password(defaultPassword)
                .role(defaultRole)
                .build();
    }

    public static LoginDTO buildLoginDTO() {
        return LoginDTO.builder()
                .email(defaultEmail)
                .password(defaultPassword)
                .build();
    }

}
