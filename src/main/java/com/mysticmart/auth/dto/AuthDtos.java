package com.mysticmart.auth.dto;
import com.mysticmart.auth.model.User;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
public class AuthDtos {
    @Data public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank @Size(min=6) private String password;
    }
    @Data public static class RegisterRequest {
        @NotBlank @Size(min=2,max=100) private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min=6) private String password;
        private User.Role role = User.Role.STAFF;
    }
    @Data @Builder public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private UserDto user;
    }
    @Data @Builder public static class UserDto {
        private Long id;
        private String email;
        private String fullName;
        private User.Role role;
    }
    @Data public static class RefreshRequest {
        @NotBlank private String refreshToken;
    }
}
