package com.example.demo.repositories;

import com.example.demo.entities.ConfirmationToken;
import com.example.demo.entities.Order;
import com.example.demo.entities.user.Customer;
import com.example.demo.entities.user.User;
import com.example.demo.enums.OrderStatus;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ConfirmationTokenRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    private User user = TestDataBuilder.buildUserNoId();
    private ConfirmationToken confirmationToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.save(user);
        confirmationToken = new ConfirmationToken(user);
    }

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
        confirmationTokenRepository.deleteAll();
    }

    @Test
    void givenConfirmationToken_whenFindByConfirmationToken_thenReturnOptionalConfirmationToken() {
        confirmationTokenRepository.save(confirmationToken);
        Optional<ConfirmationToken> result = confirmationTokenRepository
                .findByConfirmationToken(confirmationToken.getConfirmationToken());
        assertEquals(Optional.of(confirmationToken), result);
    }

    @Test
    void givenNoConfirmationToken_whenFindByConfirmationToken_thenReturnOptionalEmpty() {
        Optional<ConfirmationToken> result = confirmationTokenRepository
                .findByConfirmationToken(UUID.randomUUID());
        assertEquals(Optional.empty(), result);
    }

    @Test
    void givenConfirmation_whenFindByUser_thenReturnOptionalConfirmationToken() {
        confirmationTokenRepository.save(confirmationToken);
        ConfirmationToken result = confirmationTokenRepository.findByUser(user);
        assertEquals(confirmationToken, result);
    }

}