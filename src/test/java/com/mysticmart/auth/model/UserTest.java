package com.mysticmart.auth.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

class UserTest {

    // ── Builder ───────────────────────────────────────────────────────────

    @Test
    void builder_allFields_createsUserCorrectly() {
        User user = User.builder()
                .id(1L)
                .email("admin@mysticmart.com")
                .passwordHash("$hashed$")
                .fullName("System Admin")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("admin@mysticmart.com");
        assertThat(user.getPasswordHash()).isEqualTo("$hashed$");
        assertThat(user.getFullName()).isEqualTo("System Admin");
        assertThat(user.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void builder_defaultActive_isTrueWhenNotSet() {
        User user = User.builder()
                .email("staff@mysticmart.com")
                .passwordHash("$hashed$")
                .fullName("Staff User")
                .role(User.Role.STAFF)
                .build();  // active not set

        assertThat(user.isActive()).isTrue(); // @Builder.Default = true
    }

    @Test
    void builder_activeSetToFalse_isInactive() {
        User user = User.builder()
                .email("inactive@mysticmart.com")
                .passwordHash("$hashed$")
                .fullName("Inactive User")
                .role(User.Role.STAFF)
                .active(false)
                .build();

        assertThat(user.isActive()).isFalse();
    }

    // ── NoArgsConstructor ─────────────────────────────────────────────────

    @Test
    void noArgsConstructor_createsEmptyUser() {
        User user = new User();

        assertThat(user.getId()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getFullName()).isNull();
        assertThat(user.getRole()).isNull();
    }

    // ── AllArgsConstructor ────────────────────────────────────────────────

    @Test
    void allArgsConstructor_setsAllFields() {
        LocalDateTime now = LocalDateTime.now();

        User user = new User(1L, "mgr@mysticmart.com", "$hash$",
                "Manager One", User.Role.MANAGER, true, now);

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("mgr@mysticmart.com");
        assertThat(user.getPasswordHash()).isEqualTo("$hash$");
        assertThat(user.getFullName()).isEqualTo("Manager One");
        assertThat(user.getRole()).isEqualTo(User.Role.MANAGER);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    // ── Setter ────────────────────────────────────────────────────────────

    @Test
    void setter_updatesFieldsCorrectly() {
        User user = new User();
        user.setEmail("updated@mysticmart.com");
        user.setFullName("Updated Name");
        user.setPasswordHash("$new_hash$");
        user.setRole(User.Role.MANAGER);
        user.setActive(false);

        assertThat(user.getEmail()).isEqualTo("updated@mysticmart.com");
        assertThat(user.getFullName()).isEqualTo("Updated Name");
        assertThat(user.getPasswordHash()).isEqualTo("$new_hash$");
        assertThat(user.getRole()).isEqualTo(User.Role.MANAGER);
        assertThat(user.isActive()).isFalse();
    }

    // ── Role Enum ─────────────────────────────────────────────────────────

    @Test
    void role_allValuesExist() {
        assertThat(User.Role.values())
                .containsExactlyInAnyOrder(
                        User.Role.ADMIN,
                        User.Role.MANAGER,
                        User.Role.STAFF
                );
    }

    @Test
    void role_adminUser_hasCorrectRole() {
        User user = User.builder()
                .email("admin@mysticmart.com")
                .passwordHash("$hash$")
                .fullName("Admin")
                .role(User.Role.ADMIN)
                .build();

        assertThat(user.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(user.getRole().name()).isEqualTo("ADMIN");
    }

    @Test
    void role_valueOf_returnsCorrectEnum() {
        assertThat(User.Role.valueOf("ADMIN")).isEqualTo(User.Role.ADMIN);
        assertThat(User.Role.valueOf("MANAGER")).isEqualTo(User.Role.MANAGER);
        assertThat(User.Role.valueOf("STAFF")).isEqualTo(User.Role.STAFF);
    }

    @Test
    void role_invalidValue_throwsException() {
        assertThatThrownBy(() -> User.Role.valueOf("SUPERADMIN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── CreatedAt ─────────────────────────────────────────────────────────

    @Test
    void createdAt_canBeSetAndRetrieved() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setCreatedAt(now);

        assertThat(user.getCreatedAt()).isEqualTo(now);
    }

    // ── Edge Cases ────────────────────────────────────────────────────────

    @Test
    void email_canBeUpdatedAfterCreation() {
        User user = User.builder()
                .email("old@mysticmart.com")
                .passwordHash("$hash$")
                .fullName("User")
                .role(User.Role.STAFF)
                .build();

        user.setEmail("new@mysticmart.com");

        assertThat(user.getEmail()).isEqualTo("new@mysticmart.com");
    }

    @Test
    void twoUsers_withSameData_areNotSameInstance() {
        User user1 = User.builder().email("a@x.com").passwordHash("$h$")
                .fullName("A").role(User.Role.STAFF).build();

        User user2 = User.builder().email("a@x.com").passwordHash("$h$")
                .fullName("A").role(User.Role.STAFF).build();

        assertThat(user1).isNotSameAs(user2);
        assertThat(user1.getEmail()).isEqualTo(user2.getEmail());
    }
}