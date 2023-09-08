package com.example.demo.config;

import com.example.demo.config.exceptions.UserNotEnabledException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.example.demo.config.utils.GetCurrentUser.getCurrentUserDetails;

@Component
public class CustomFilter extends OncePerRequestFilter {

    private final RequestMatcher productMatcher =
            new AntPathRequestMatcher("/products", HttpMethod.POST.name());

    private final RequestMatcher paymentMatcher =
            new AntPathRequestMatcher("/payment", HttpMethod.POST.name());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UserDetails user = getCurrentUserDetails();

        if (!user.isEnabled()) {
            throw new UserNotEnabledException();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        RequestMatcher productRequestMatcher = new NegatedRequestMatcher(productMatcher);
        RequestMatcher paymentRequestMatcher = new NegatedRequestMatcher(paymentMatcher);

        return productRequestMatcher.matches(request) && paymentRequestMatcher.matches(request);
    }

}