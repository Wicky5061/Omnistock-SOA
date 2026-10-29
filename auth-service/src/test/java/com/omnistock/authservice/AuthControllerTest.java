package com.omnistock.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.authservice.dto.LoginRequest;
import com.omnistock.authservice.dto.RegisterRequest;
import com.omnistock.authservice.repository.UserRepository;
import com.omnistock.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_withValidDetails_shouldReturn201WithToken() throws Exception {
        RegisterRequest req = new RegisterRequest("alice", "password123", "ROLE_USER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("alice")))
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    void register_withDuplicateUsername_shouldReturn409() throws Exception {
        RegisterRequest req1 = new RegisterRequest("bob", "password123", "ROLE_USER");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        RegisterRequest req2 = new RegisterRequest("bob", "differentPass", "ROLE_USER");
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already registered")));
    }

    @Test
    void register_withShortPassword_shouldReturn400() throws Exception {
        RegisterRequest req = new RegisterRequest("charlie", "12", "ROLE_USER");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("at least 4 characters")));
    }

    @Test
    void login_withCorrectCredentials_shouldReturn200WithToken() throws Exception {
        RegisterRequest reg = new RegisterRequest("dave", "securePass", "ROLE_USER");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest("dave", "securePass");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("dave")))
                .andExpect(jsonPath("$.type", is("Bearer")));
    }

    @Test
    void login_withWrongPassword_shouldReturn401() throws Exception {
        RegisterRequest reg = new RegisterRequest("eve", "correctPassword", "ROLE_USER");
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        LoginRequest login = new LoginRequest("eve", "wrongPassword");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid username or password")));
    }

    @Test
    void login_withUnknownUser_shouldReturn401() throws Exception {
        LoginRequest login = new LoginRequest("nonexistent", "somePassword");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid username or password")));
    }

    @Test
    void validateToken_withValidToken_shouldReturn200() throws Exception {
        String token = jwtUtil.generateToken("frank", "ROLE_USER");

        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.username", is("frank")));
    }

    @Test
    void validateToken_withMissingToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/auth/validate"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_withInvalidToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/auth/validate")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid or expired")));
    }
}
