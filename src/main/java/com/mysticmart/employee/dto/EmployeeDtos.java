package com.mysticmart.employee.dto;
import com.mysticmart.auth.model.User;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class EmployeeDtos {
    @Data public static class CreateRequest {
        @NotBlank @Size(min=2,max=100) private String fullName;
        @Email @NotBlank private String email;
        @NotBlank @Size(min=6) private String password;
        private User.Role role = User.Role.STAFF;
        @Size(max=20) private String phone;
        @Size(max=100) private String department;
        @DecimalMin("0") private BigDecimal salary;
        private LocalDate hireDate;
        @Size(max=255) private String address;
    }
    @Data public static class UpdateRequest {
        @Size(min=2,max=100) private String fullName;
        @Size(max=20) private String phone;
        @Size(max=100) private String department;
        @DecimalMin("0") private BigDecimal salary;
        private LocalDate hireDate;
        @Size(max=255) private String address;
    }
    @Data public static class RoleRequest { @NotNull private User.Role role; }
    @Data @Builder public static class EmployeeResponse {
        private Long id; private Long userId; private String fullName; private String email;
        private User.Role role; private String phone; private String department;
        private BigDecimal salary; private LocalDate hireDate; private String address;
        private boolean active; private LocalDateTime createdAt;
    }
}
