package com.mysticmart.employee.model;
import com.mysticmart.auth.model.User;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.EAGER) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 100) private String fullName;
    @Column(length = 20) private String phone;
    @Column(length = 100) private String department;
    @Column(precision = 10, scale = 2) private BigDecimal salary;
    private LocalDate hireDate;
    @Column(length = 255) private String address;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    @LastModifiedDate private LocalDateTime updatedAt;
}
