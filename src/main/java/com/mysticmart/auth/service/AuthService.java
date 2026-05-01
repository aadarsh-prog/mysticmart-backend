package com.mysticmart.auth.service;
import com.mysticmart.auth.dto.AuthDtos;
import com.mysticmart.auth.model.User;
import com.mysticmart.auth.repository.UserRepository;
import com.mysticmart.auth.security.JwtUtil;
import com.mysticmart.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        var user = userRepository.findByEmail(req.getEmail()).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        var ud = userDetailsService.loadUserByUsername(req.getEmail());
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(ud))
                .refreshToken(jwtUtil.generateRefreshToken(ud))
                .tokenType("Bearer")
                .user(toDto(user)).build();
    }

    @Transactional
    public User register(AuthDtos.RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new AppException("Email already in use", HttpStatus.CONFLICT);
        return userRepository.save(User.builder()
                .fullName(req.getFullName()).email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole()).active(true).build());
    }

    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest req) {
        String email = jwtUtil.extractUsername(req.getRefreshToken());
        var ud = userDetailsService.loadUserByUsername(email);
        if (!jwtUtil.isTokenValid(req.getRefreshToken(), ud))
            throw new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(ud))
                .refreshToken(jwtUtil.generateRefreshToken(ud))
                .tokenType("Bearer").user(toDto(user)).build();
    }

    public AuthDtos.UserDto getMe(String email) {
        return toDto(userRepository.findByEmail(email).orElseThrow(() -> new AppException("Not found", HttpStatus.NOT_FOUND)));
    }

    private AuthDtos.UserDto toDto(User u) {
        return AuthDtos.UserDto.builder().id(u.getId()).email(u.getEmail()).fullName(u.getFullName()).role(u.getRole()).build();
    }
}
