package com.example.demo.services;

import com.example.demo.entities.user.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public <T extends User> T findByIdAndEnsureType(UUID userId, Class<T> expectedClass){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(userId));

        if (!expectedClass.isInstance(user)){
            throw new ClassCastException("Invalid user type. Cannot cast user to the specified type.");
        }

        return expectedClass.cast(user);
    }

}
