package com.example.demo.config;


import com.example.demo.entities.user.User;
import com.example.demo.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getPrincipal().toString();
        String password = authentication.getCredentials().toString();

        User user = authenticationService.loadUserByUsername(email);

        if (passwordEncoder.matches(password, user.getPassword()) && user.isAccountNonLocked()) {
            if (user.getFailedAttempt() > 0) {
                authenticationService.resetFailedAttempts(user.getEmail());
            }
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        }

        if (user.isAccountNonLocked()) {
            if (user.getFailedAttempt() < AuthenticationService.MAX_FAILED_ATTEMPTS - 1) {
                authenticationService.increaseFailedAttempts(user);
                throw new BadCredentialsException("Wrong password or username.");
            }
            authenticationService.lock(user);
            throw new LockedException("Your account has been locked due to 3 failed login attempts."
                    + " It will be unlocked after 5 minutes.");
        } else {
            if (authenticationService.isLockTimeExpired(user)) {
                throw new LockedException("Your account has been unlocked. Please try to login again.");
            }
            throw new LockedException("Your account has been locked due to 3 failed login attempts. " +
                    "Please try again later.");
        }
    }


    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
