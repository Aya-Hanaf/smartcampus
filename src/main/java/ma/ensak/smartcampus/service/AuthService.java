package ma.ensak.smartcampus.service;

import ma.ensak.smartcampus.dto.LoginRequest;
import ma.ensak.smartcampus.dto.LoginResponse;
import ma.ensak.smartcampus.exception.AccountDisabledException;
import ma.ensak.smartcampus.exception.InvalidCredentialsException;
import ma.ensak.smartcampus.model.AuditEvent;
import ma.ensak.smartcampus.model.AuditEventType;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import ma.ensak.smartcampus.config.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       AuditEventRepository auditEventRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * noRollbackFor est indispensable : sans lui, l'exception annulerait la transaction
     * et le compteur d'échecs comme l'AuditEvent ne seraient jamais enregistrés.
     */
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        user.setFailedLogins(0);
        userRepository.save(user);

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }

    private void registerFailedAttempt(User user) {
        int failures = user.getFailedLogins() + 1;
        user.setFailedLogins(failures);
        userRepository.save(user);

        if (failures == MAX_FAILED_ATTEMPTS) {
            auditEventRepository.save(new AuditEvent(
                    user,
                    AuditEventType.LOGIN_FAILED_3_TIMES,
                    MAX_FAILED_ATTEMPTS + " échecs de connexion consécutifs"));
        }
    }
}