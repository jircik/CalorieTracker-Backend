package com.jircik.calorietrackerapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jircik.calorietrackerapi.domain.dto.request.LoginRequest;
import com.jircik.calorietrackerapi.domain.dto.request.RegisterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.AuthResponse;
import com.jircik.calorietrackerapi.security.JwtService;
import com.jircik.calorietrackerapi.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register — deve criar usuário e retornar 201 com token")
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("João", "joao@email.com", "senha1234");
        AuthResponse response = new AuthResponse("jwt-token", 42L, "João", "joao@email.com");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.email").value("joao@email.com"));
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 400 quando email é inválido")
    void register_shouldReturn400WhenEmailInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest("João", "not-an-email", "senha1234");

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 400 quando senha é muito curta")
    void register_shouldReturn400WhenPasswordTooShort() throws Exception {
        RegisterRequest request = new RegisterRequest("João", "joao@email.com", "123");

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 400 quando email já está em uso")
    void register_shouldReturn400WhenEmailInUse() throws Exception {
        RegisterRequest request = new RegisterRequest("João", "joao@email.com", "senha1234");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already in use"));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/login — deve autenticar e retornar 200 com token")
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest("joao@email.com", "senha1234");
        AuthResponse response = new AuthResponse("jwt-token", 42L, "João", "joao@email.com");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("POST /auth/login — deve retornar 400 quando email é em branco")
    void login_shouldReturn400WhenEmailBlank() throws Exception {
        LoginRequest request = new LoginRequest("", "senha1234");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
