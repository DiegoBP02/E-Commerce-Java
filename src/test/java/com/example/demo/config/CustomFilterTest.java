package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.config.exceptions.UserNotEnabledException;
import com.example.demo.entities.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomFilterTest extends ApplicationConfigTest {

    @Autowired
    private CustomFilter customFilter;

    private Authentication authentication;
    private SecurityContext securityContext;
    private User user = mock(User.class);
    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private FilterChain filterChain = mock(FilterChain.class);

    void setupSecurityContext() {
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    void verifyAuthentication() {
        verify(authentication, times(1)).getPrincipal();
        verify(securityContext, times(1)).getAuthentication();
    }

    @Test
    void givenUserIsNotEnabled_whenDoFilterInternal_thenThrowUserNotEnabledException()
            throws ServletException, IOException {
        setupSecurityContext();
        when(user.isEnabled()).thenReturn(false);

        assertThrows(UserNotEnabledException.class,
                () -> customFilter.doFilterInternal(request, response, filterChain));

        verifyNoInteractions(filterChain);
        verifyAuthentication();
    }

    @Test
    void givenUserIsEnabled_whenDoFilterInternal_thenDoFilter()
            throws ServletException, IOException {
        setupSecurityContext();
        when(user.isEnabled()).thenReturn(true);

        customFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyAuthentication();
    }

    @Test
    void givenProductEndpointPOST_whenShouldNotFilter_thenReturnFalse() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/products");

        boolean result = customFilter.shouldNotFilter(request);

        assertFalse(result);
    }

    @Test
    void givenPaymentEndpointPOST_whenShouldNotFilter_thenReturnFalse() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/payment");

        boolean result = customFilter.shouldNotFilter(request);

        assertFalse(result);
    }

    @Test
    void givenAnotherEndpointPOST_whenShouldNotFilter_thenReturnTrue() throws ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/orders");

        boolean result = customFilter.shouldNotFilter(request);

        assertTrue(result);
    }

}