package com.example.demo.entities.user;

import com.example.demo.enums.Role;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity
public class Seller extends User {

    @Builder
    public Seller(String name, String email, String password, Role role) {
        super(name, email, password, role);
    }

}
