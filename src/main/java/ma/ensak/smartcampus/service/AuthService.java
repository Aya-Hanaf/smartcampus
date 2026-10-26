package ma.ensak.smartcampus.service;

import ma.ensak.smartcampus.config.JwtService;
import ma.ensak.smartcampus.dto.LoginRequest;
import ma.ensak.smartcampus.dto.LoginResponse;
import ma.ensak.smartcampus.exception.AccountDisabledException;
import ma.ensak.smartcampus.exception.AccountLockedException;
import ma.ensak.smartcampus.exception.InvalidCredentialsException;
import ma.ensak.smartcampus.model.AuditEvent;
import ma.ensak.smartcampus.model.AuditEventType;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ma.ensak.smartcampus.dto.CurrentUserResponse;

import java.time.LocalDateTime;

@Service
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long lockDurationMinutes;

    public AuthService(UserRepository userRepository,
                       AuditEventRepository auditEventRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.security.lock-duration-minutes:15}") long lockDurationMinutes) {
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    /**
     * noRollbackFor est indispensable : sans lui, l'exception annulerait la transaction
     * et ni le compteur d'échecs, ni le verrouillage, ni l'AuditEvent ne seraient enregistrés
     * (cause du bug SC-13).
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(InvalidCredentialsException::new);

        LocalDateTime now = LocalDateTime.now();

        if (user.isLockedAt(now)) {
            throw new AccountLockedException();
        }

        if (user.getLockedUntil() != null) {
            // le verrouillage a expiré : on repart de zéro
            user.setLockedUntil(null);
            user.setFailedLogins(0);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user, now);
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new AccountDisabledException();
        }

        user.setFailedLogins(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return new LoginResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getEmail(),
                user.getFullName(),
                user.getRole());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);
        return new CurrentUserResponse(user.getEmail(), user.getFullName(), user.getRole());
    }

    private void registerFailedAttempt(User user, LocalDateTime now) {
        int failures = user.getFailedLogins() + 1;
        user.setFailedLogins(failures);

        if (failures >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plusMinutes(lockDurationMinutes));
            userRepository.save(user);
            auditEventRepository.save(new AuditEvent(
                    user,
                    AuditEventType.LOGIN_FAILED_3_TIMES,
                    MAX_FAILED_ATTEMPTS + " échecs de connexion consécutifs : compte verrouillé "
                            + lockDurationMinutes + " min"));
            throw new AccountLockedException();
        }

        userRepository.save(user);
    }
}