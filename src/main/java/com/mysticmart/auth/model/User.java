package com.mysticmart.auth.model;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, nullable = false, length = 100) private String email;
    @Column(nullable = false) private String passwordHash;
    @Column(nullable = false, length = 100) private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role;
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @CreatedDate @Column(updatable = false) private LocalDateTime createdAt;
    public enum Role { ADMIN, MANAGER, STAFF }
}
