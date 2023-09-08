package com.example.demo.entities;

import com.example.demo.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class ConfirmationToken {
    public static final int EXPIRATION_TIME_IN_SECONDS = 30 * 60; // 30min

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "confirmation_token", unique = true,nullable = false)
    private UUID confirmationToken;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT")
    @Column(nullable = false)
    private Instant expiryDate;

    @OneToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public ConfirmationToken(User user) {
        this.user = user;
        createdDate = Instant.now();
        confirmationToken = UUID.randomUUID();
        this.expiryDate = createdDate.plusSeconds(EXPIRATION_TIME_IN_SECONDS);
    }

    public boolean isTokenExpired() {
        Instant currentTime = Instant.now();
        return currentTime.isAfter(expiryDate);
    }

    public void resetToken(){
        this.createdDate = Instant.now();
        this.confirmationToken = UUID.randomUUID();
        this.expiryDate = Instant.now().plusSeconds(EXPIRATION_TIME_IN_SECONDS);
    }

    public long getTimeUntilExpiration() {
        Instant currentTime = Instant.now();
        long secondsUntilExpiration = currentTime.until(expiryDate, ChronoUnit.SECONDS);
        return TimeUnit.SECONDS.toMinutes(secondsUntilExpiration);
    }
}
