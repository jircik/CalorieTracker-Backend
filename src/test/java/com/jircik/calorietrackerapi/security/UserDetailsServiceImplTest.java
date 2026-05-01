package com.jircik.calorietrackerapi.security;

import com.jircik.calorietrackerapi.domain.entity.RoleEnum;
import com.jircik.calorietrackerapi.domain.entity.User;
import com.jircik.calorietrackerapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("deve retornar UserDetails quando email existe")
    void shouldReturnUserDetailsWhenEmailFound() {
        User user = User.builder()
                .id(1L)
                .name("João")
                .email("joao@email.com")
                .password("encoded-password")
                .role(RoleEnum.USER)
                .build();
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("joao@email.com");

        assertThat(details.getUsername()).isEqualTo("joao@email.com");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("deve mapear ROLE_ADMIN quando usuário é ADMIN")
    void shouldMapAdminRole() {
        User admin = User.builder()
                .id(1L).name("Admin").email("admin@email.com")
                .password("hash").role(RoleEnum.ADMIN).build();
        when(userRepository.findByEmail("admin@email.com")).thenReturn(Optional.of(admin));

        UserDetails details = userDetailsService.loadUserByUsername("admin@email.com");

        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("deve lançar UsernameNotFoundException quando email não existe")
    void shouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("missing@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("missing@email.com");
    }
}
