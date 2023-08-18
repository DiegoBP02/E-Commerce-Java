package com.example.demo.services.utils;

import com.example.demo.entities.user.User;
import com.example.demo.services.exceptions.UnauthorizedAccessException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CheckOwnership {
    public static boolean checkOwnership(User user, UUID objAuthorId) {
        UUID userId = user.getId();
        if (userId.equals(objAuthorId)) return true;
        throw new UnauthorizedAccessException
                ("You are not authorized to access this object. It does not belong to you");
    }
}
