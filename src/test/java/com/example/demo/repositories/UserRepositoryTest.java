package com.example.demo.repositories;

import com.example.demo.entities.ResetPasswordToken;
import com.example.demo.entities.user.User;
import com.example.demo.utils.TestDataBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User user = TestDataBuilder.buildUserNoId();
    private ResetPasswordToken resetPasswordToken = TestDataBuilder.buildResetPasswordTokenNoId(user);

    @AfterEach
    void tearDown() throws Exception {
        userRepository.deleteAll();
    }

    @Test
    void givenUser_whenFindByEmail_thenReturnOptionalUser() {
        userRepository.save(user);
        Optional<User> result = userRepository.findByEmail(user.getEmail());
        assertEquals(Optional.of(user), result);
    }

    @Test
    void givenNoUser_whenFindByEmail_thenReturnOptionalEmpty() {
        Optional<User> result = userRepository.findByEmail("random");
        assertEquals(Optional.empty(), result);
    }

    @Test
    void givenResetPasswordToken_whenFindByResetPasswordTokenResetPasswordToken_thenReturnOptionalUser() {
        user.setResetPasswordToken(resetPasswordToken);
        userRepository.save(user);
        Optional<User> result = userRepository
                .findByResetPasswordTokenResetPasswordToken(resetPasswordToken.getResetPasswordToken());
        assertEquals(Optional.of(user), result);
    }

}