package com.example.demo.repositories;

import com.example.demo.entities.user.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Transactional
    @Query("UPDATE User SET failedAttempt = ?1 WHERE email = ?2")
    @Modifying
    void updateFailedAttempts(int failAttempts, String email);

    Optional<User> findByResetPasswordTokenResetPasswordToken(UUID token);
}
