package com.example.demo.controller;

import com.example.demo.ApplicationConfigTest;
import com.example.demo.config.RateLimitInterceptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public abstract class ApplicationConfigTestController extends ApplicationConfigTest {

    private final String PATH;

    public ApplicationConfigTestController(String path) {
        this.PATH = path;
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void disableRateLimitInterceptor() throws Exception {
        when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    MockHttpServletRequestBuilder mockPostRequest
            () throws Exception {
        return MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockPostRequest
            (String endpoint) throws Exception {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockPostRequest
            (Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    MockHttpServletRequestBuilder mockPostRequest
            (String endpoint, Object requestObject) throws Exception {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    MockHttpServletRequestBuilder mockPostRequestWithParams
            (String endpoint, String paramName, String paramValue)
            throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .param(paramName, paramValue)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockPostRequestWithParams
            (String endpoint, Object requestObject, String paramName, String paramValue)
            throws JsonProcessingException {
        return MockMvcRequestBuilders
                .post(PATH + "/" + endpoint)
                .param(paramName, paramValue)
                .content(this.objectMapper.writeValueAsString(requestObject))
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockGetRequest() {
        return MockMvcRequestBuilders
                .get(PATH)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockGetRequest(String endpoint) {
        return MockMvcRequestBuilders
                .get(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockGetRequestWithParams
            (String endpoint, String paramName, String paramValue)
            throws JsonProcessingException {
        return MockMvcRequestBuilders
                .get(PATH + "/" + endpoint)
                .param(paramName, paramValue)
                .contentType(MediaType.APPLICATION_JSON);
    }

    MockHttpServletRequestBuilder mockPatchRequest
            (String endpoint, Object requestObject) throws JsonProcessingException {
        return MockMvcRequestBuilders
                .patch(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(requestObject));
    }

    MockHttpServletRequestBuilder mockDeleteRequest(String endpoint) {
        return MockMvcRequestBuilders
                .delete(PATH + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON);
    }
}
