package ma.ensak.smartcampus.config;

import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("APP_ADMIN_PASSWORD trop court ({} caractères minimum) : administrateur non créé.",
                    MIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.findByEmailIgnoreCase(email.trim()).isPresent()) {
            return;
        }
        userRepository.save(new User(email.trim().toLowerCase(), passwordEncoder.encode(password),
                "Administrateur", Role.ADMIN));
        log.info("Compte administrateur initial créé : {}", email.trim().toLowerCase());
    }
}