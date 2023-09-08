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
public class Admin extends User {

    @Builder
    public Admin(String name, String email, String password) {
        super(name, email, password, Role.Admin);
    }

}
