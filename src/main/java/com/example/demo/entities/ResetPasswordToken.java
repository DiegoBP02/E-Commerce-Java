package com.example.demo.entities;

import com.example.demo.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class ResetPasswordToken {
    public static final int EXPIRATION_TIME_IN_SECONDS = 30 * 60; // 30min

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private UUID resetPasswordToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant expiryDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public ResetPasswordToken(User user) {
        this.user = user;
        createdDate = Instant.now();
        resetPasswordToken = UUID.randomUUID();
        this.expiryDate = createdDate.plusSeconds(EXPIRATION_TIME_IN_SECONDS);
    }

    public boolean isTokenExpired() {
        Instant currentTime = Instant.now();
        return currentTime.isAfter(expiryDate);
    }

    public void resetToken() {
        this.createdDate = Instant.now();
        this.resetPasswordToken = UUID.randomUUID();
        this.expiryDate = Instant.now().plusSeconds(EXPIRATION_TIME_IN_SECONDS);
    }
}
