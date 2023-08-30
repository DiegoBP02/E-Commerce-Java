package com.example.demo.entities.user;

import com.example.demo.entities.Product;
import com.example.demo.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Seller extends User {

    @ToString.Exclude
    @JsonIgnore
    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Product> products = new ArrayList<>();

    @Builder
    public Seller(String name, String email, String password, Role role) {
        super(name, email, password, role);
    }

}
