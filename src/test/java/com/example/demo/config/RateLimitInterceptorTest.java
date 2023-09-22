package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.controller.exceptions.RateLimitException;
import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RateLimitInterceptorTest extends ApplicationConfigTest {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    private Bucket bucket = mock(Bucket.class);
    private MockHttpServletRequest request = new MockHttpServletRequest();
    private MockHttpServletResponse response = new MockHttpServletResponse();
    private Object handler = mock(Object.class);

    @Test
    void givenRateLimitNotExceeded_whenPreHandle_thenReturnTrueAndHeaderOfRemainingTokens() throws Exception {
        boolean result = rateLimitInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertEquals(99, Integer.parseInt(response.getHeader("X-Rate-Limit-Remaining")));
    }

    @Test
    void givenRateLimitIsExceeded_whenPreHandle_thenReturnTrueAndHeaderOfRemainingTokens() throws Exception {


        assertThrows(RateLimitException.class,
                () -> {
                    for (int i = 0; i < 150; i++) {
                        rateLimitInterceptor.preHandle(request, response, handler);
                    }
                });

        assertNotNull(response.getHeader("X-Rate-Limit-Retry-After-Seconds"));
    }

}