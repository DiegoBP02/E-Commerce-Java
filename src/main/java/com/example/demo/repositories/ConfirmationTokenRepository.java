package com.example.demo.repositories;

import com.example.demo.entities.ConfirmationToken;
import com.example.demo.entities.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConfirmationTokenRepository extends JpaRepository<ConfirmationToken, UUID> {
    Optional<ConfirmationToken> findByConfirmationToken(UUID confirmationToken);

    ConfirmationToken findByUser(User user);
}

