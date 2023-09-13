package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.entities.user.User;
import com.example.demo.repositories.UserRepository;
import com.example.demo.services.TokenService;
import com.example.demo.services.exceptions.UserNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class FilterTokenTest extends ApplicationConfigTest {

    @Autowired
    private FilterToken filterToken;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private User user = mock(User.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private String token = "token";
    private String subject = "subject";

    @Test
    void givenValidToken_whenDoFilterInternal_thenDoFilter() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.getSubject(token)).thenReturn(subject);
        when(userRepository.findByEmail(subject)).thenReturn(Optional.of(user));

        filterToken.doFilterInternal(request, response, filterChain);

        User checkAuthentication = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals(user, checkAuthentication);

        verify(request, times(1)).getHeader("Authorization");
        verify(tokenService, times(1)).getSubject(token);
        verify(userRepository, times(1)).findByEmail(subject);
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void givenUserNotFound_whenDoFilterInternal_thenDoFilter() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.getSubject(token)).thenReturn(subject);
        when(userRepository.findByEmail(subject)).thenReturn(Optional.empty());
        doAnswer(invocation -> {
            Exception e = invocation.getArgument(3);
            assertEquals(UserNotFoundException.class, e.getClass());
            return null;
        }).when(resolver).resolveException(eq(request), eq(response), eq(null), any(Exception.class));

        filterToken.doFilterInternal(request, response, filterChain);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        verify(request, times(1)).getHeader("Authorization");
        verify(tokenService, times(1)).getSubject(token);
        verify(userRepository, times(1)).findByEmail(subject);
        verify(resolver, times(1))
                .resolveException(eq(request), eq(response), eq(null), any(Exception.class));
        verifyNoInteractions(filterChain);
    }

}