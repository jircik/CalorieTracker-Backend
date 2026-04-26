package com.jircik.calorietrackerapi.util;

import com.jircik.calorietrackerapi.security.UserPrincipal;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class SecurityTestUtils {
    private SecurityTestUtils() {}

    public static UsernamePasswordAuthenticationToken authAs(Long userId) {
        UserPrincipal principal = new UserPrincipal(
                userId, "test@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    public static UsernamePasswordAuthenticationToken adminAuth() {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin@email.com",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }
}
