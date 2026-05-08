package com.mysticmart.auth.service;

import com.mysticmart.auth.dto.AuthDtos;
import com.mysticmart.auth.model.User;
import com.mysticmart.auth.repository.UserRepository;
import com.mysticmart.auth.security.JwtUtil;
import com.mysticmart.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthService authService;

    private User adminUser;
    private UserDetails adminUserDetails;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).email("admin@mysticmart.com")
                .passwordHash("$hashed$").fullName("System Admin")
                .role(User.Role.ADMIN).active(true).build();

        adminUserDetails = org.springframework.security.core.userdetails.User
                .withUsername("admin@mysticmart.com")
                .password("$hashed$")
                .authorities(List.of())
                .build();
    }

    // ── login ─────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() {
        when(userRepository.findByEmail("admin@mysticmart.com")).thenReturn(Optional.of(adminUser));
        when(userDetailsService.loadUserByUsername("admin@mysticmart.com")).thenReturn(adminUserDetails);
        when(jwtUtil.generateAccessToken(any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");

        var req = new AuthDtos.LoginRequest();
        req.setEmail("admin@mysticmart.com");
        req.setPassword("admin123");

        var result = authService.login(req);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(result.getUser().getEmail()).isEqualTo("admin@mysticmart.com");
        assertThat(result.getUser().getRole()).isEqualTo(User.Role.ADMIN);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_badCredentials_propagatesException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        var req = new AuthDtos.LoginRequest();
        req.setEmail("admin@mysticmart.com");
        req.setPassword("wrongpass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_userNotFound_throwsAppException() {
        // auth manager passes, but user somehow doesn't exist in DB
        when(userRepository.findByEmail("ghost@x.com")).thenReturn(Optional.empty());

        var req = new AuthDtos.LoginRequest();
        req.setEmail("ghost@x.com");
        req.setPassword("pass");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("User not found");
    }

    // ── register ──────────────────────────────────────────────────────────

    @Test
    void register_newUser_savesWithHashedPassword() {
        when(userRepository.existsByEmail("new@x.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$hashed_new$");
        when(userRepository.save(any())).thenReturn(adminUser);

        var req = new AuthDtos.RegisterRequest();
        req.setEmail("new@x.com");
        req.setPassword("password123");
        req.setFullName("New User");
        req.setRole(User.Role.STAFF);

        authService.register(req);

        verify(userRepository).save(argThat(u ->
                "$hashed_new$".equals(u.getPasswordHash())
                        && u.isActive()
                        && u.getRole() == User.Role.STAFF
        ));
    }

    @Test
    void register_duplicateEmail_throwsAppException() {
        when(userRepository.existsByEmail("admin@mysticmart.com")).thenReturn(true);

        var req = new AuthDtos.RegisterRequest();
        req.setEmail("admin@mysticmart.com");
        req.setPassword("pass"); req.setFullName("Dup");

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Email already in use");
    }

    // ── refresh ───────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_returnsNewTokens() {
        when(jwtUtil.extractUsername("valid-refresh")).thenReturn("admin@mysticmart.com");
        when(userDetailsService.loadUserByUsername("admin@mysticmart.com")).thenReturn(adminUserDetails);
        when(jwtUtil.isTokenValid("valid-refresh", adminUserDetails)).thenReturn(true);
        when(userRepository.findByEmail("admin@mysticmart.com")).thenReturn(Optional.of(adminUser));
        when(jwtUtil.generateAccessToken(any())).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh");

        var req = new AuthDtos.RefreshRequest();
        req.setRefreshToken("valid-refresh");

        var result = authService.refresh(req);

        assertThat(result.getAccessToken()).isEqualTo("new-access");
        assertThat(result.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_expiredToken_throwsAppException() {
        when(jwtUtil.extractUsername("expired-token")).thenReturn("admin@mysticmart.com");
        when(userDetailsService.loadUserByUsername("admin@mysticmart.com")).thenReturn(adminUserDetails);
        when(jwtUtil.isTokenValid("expired-token", adminUserDetails)).thenReturn(false);

        var req = new AuthDtos.RefreshRequest();
        req.setRefreshToken("expired-token");

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    // ── getMe ─────────────────────────────────────────────────────────────

    @Test
    void getMe_returnsUserDto() {
        when(userRepository.findByEmail("admin@mysticmart.com")).thenReturn(Optional.of(adminUser));

        var result = authService.getMe("admin@mysticmart.com");

        assertThat(result.getEmail()).isEqualTo("admin@mysticmart.com");
        assertThat(result.getFullName()).isEqualTo("System Admin");
        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void getMe_unknownEmail_throwsAppException() {
        when(userRepository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe("nobody@x.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Not found");
    }
}