package com.jircik.calorietrackerapi.service;

import com.jircik.calorietrackerapi.domain.dto.request.LoginRequest;
import com.jircik.calorietrackerapi.domain.dto.request.RegisterRequest;
import com.jircik.calorietrackerapi.domain.dto.response.AuthResponse;
import com.jircik.calorietrackerapi.domain.entity.RoleEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.exception.ResourceNotFoundException;
import com.jircik.calorietrackerapi.repository.UserRepository;
import com.jircik.calorietrackerapi.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register — deve criar usuário com role USER, encriptar senha e retornar token")
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest("João", "joao@email.com", "senha1234");
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha1234")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateToken("joao@email.com", 42L)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(42L);
        assertThat(response.name()).isEqualTo("João");
        assertThat(response.email()).isEqualTo("joao@email.com");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(RoleEnum.USER);
    }

    @Test
    @DisplayName("register — deve lançar exceção quando email já está em uso")
    void register_shouldThrowWhenEmailAlreadyInUse() {
        RegisterRequest request = new RegisterRequest("João", "joao@email.com", "senha1234");
        when(userRepository.findByEmail("joao@email.com"))
                .thenReturn(Optional.of(User.builder().id(1L).build()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login — deve autenticar e retornar token")
    void login_shouldReturnToken() {
        LoginRequest request = new LoginRequest("joao@email.com", "senha1234");
        User user = User.builder()
                .id(42L).name("João").email("joao@email.com")
                .password("hash").role(RoleEnum.USER).build();
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("joao@email.com", 42L)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(42L);
        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("login — deve propagar exceção quando credenciais são inválidas")
    void login_shouldThrowOnBadCredentials() {
        LoginRequest request = new LoginRequest("joao@email.com", "senha-errada");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login — deve lançar ResourceNotFoundException quando usuário não existe após auth")
    void login_shouldThrowWhenUserMissingAfterAuth() {
        LoginRequest request = new LoginRequest("ghost@email.com", "senha1234");
        when(userRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }
}
