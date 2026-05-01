package com.mysticmart.config;
import com.mysticmart.auth.model.User;
import com.mysticmart.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin@mysticmart.com","admin123","System Admin", User.Role.ADMIN);
        seed("manager@mysticmart.com","manager123","Store Manager", User.Role.MANAGER);
        seed("cashier@mysticmart.com","cashier123","Cashier Staff", User.Role.STAFF);
    }
    private void seed(String email, String pw, String name, User.Role role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder().email(email).passwordHash(passwordEncoder.encode(pw))
                    .fullName(name).role(role).active(true).build());
            log.info("Seeded: {} / {}", email, pw);
        }
    }
}
