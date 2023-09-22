package com.example.demo.config;

import com.example.demo.ApplicationConfigTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.mockito.Mockito.*;

class MvcConfigTest extends ApplicationConfigTest {

    @Autowired
    private MvcConfig mvcConfig;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    private InterceptorRegistry interceptorRegistry = mock(InterceptorRegistry.class);
    private InterceptorRegistration interceptorRegistration = mock(InterceptorRegistration.class);

    @Test
    void givenInterceptorRegister_whenAddInterceptors_thenAddRateLimitInterceptorAndAddAllRoutesToPathPatterns() {
        when(interceptorRegistry.addInterceptor(rateLimitInterceptor)).thenReturn(interceptorRegistration);

        mvcConfig.addInterceptors(interceptorRegistry);

        verify(interceptorRegistry, times(1)).addInterceptor(rateLimitInterceptor);
        verify(interceptorRegistration, times(1)).addPathPatterns("/**");
    }

}