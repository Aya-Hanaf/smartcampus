package ma.ensak.smartcampus.service;

import ma.ensak.smartcampus.config.JwtService;
import ma.ensak.smartcampus.dto.LoginRequest;
import ma.ensak.smartcampus.dto.LoginResponse;
import ma.ensak.smartcampus.exception.AccountDisabledException;
import ma.ensak.smartcampus.exception.AccountLockedException;
import ma.ensak.smartcampus.exception.InvalidCredentialsException;
import ma.ensak.smartcampus.model.AuditEvent;
import ma.ensak.smartcampus.model.AuditEventType;
import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests unitaires SC-12 du service d'authentification (mocks, sans Spring). */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "etudiant.test@ensak.ma";
    private static final String PASSWORD = "MotDePasse-Test-123";
    private static final String WRONG = "mauvais";
    private static final String TOKEN = "jwt-test";
    private static final long LOCK_MIN = 15L;

    @Mock
    private UserRepository users;
    @Mock
    private AuditEventRepository events;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtService jwt;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, events, encoder, jwt, LOCK_MIN);
    }

    private User activeUser() {
        return new User(EMAIL, "hash", "Etudiant Test", Role.STUDENT);
    }

    private LoginRequest req(String email, String password) {
        return new LoginRequest(email, password);
    }

    @Test
    void successReturnsJwtAndResetsFailures() {
        User user = activeUser();
        user.setFailedLogins(2);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(jwt.generateToken(user)).thenReturn(TOKEN);
        when(jwt.getExpirationSeconds()).thenReturn(3600L);
        LoginResponse res = authService.login(req(EMAIL, PASSWORD));
        assertThat(res.token()).isEqualTo(TOKEN);
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(user.getFailedLogins()).isZero();
        verify(users).save(user);
        verify(events, never()).save(any());
    }

    @Test
    void emailIsTrimmedBeforeLookup() {
        User user = activeUser();
        when(users.findByEmailIgnoreCase(EMAIL.toUpperCase())).thenReturn(Optional.of(user));
        when(encoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(jwt.generateToken(user)).thenReturn(TOKEN);
        authService.login(req("  " + EMAIL.toUpperCase() + "  ", PASSWORD));
        verify(users).findByEmailIgnoreCase(EMAIL.toUpperCase());
    }

    @Test
    void unknownEmailThrowsWithoutSideEffects() {
        when(users.findByEmailIgnoreCase("inconnu@ensak.ma")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.login(req("inconnu@ensak.ma", PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(encoder, never()).matches(any(), any());
        verify(users, never()).save(any());
        verify(events, never()).save(any());
    }

    @Test
    void failuresIncrementCounterWithoutLocking() {
        User user = activeUser();
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches(WRONG, "hash")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(req(EMAIL, WRONG)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getFailedLogins()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        assertThatThrownBy(() -> authService.login(req(EMAIL, WRONG)))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(user.getFailedLogins()).isEqualTo(2);
        verify(events, never()).save(any());
    }

    @Test
    void thirdFailureLocksAndRecordsEvent() {
        User user = activeUser();
        user.setFailedLogins(2);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches(WRONG, "hash")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(req(EMAIL, WRONG)))
                .isInstanceOf(AccountLockedException.class);
        assertThat(user.getFailedLogins()).isEqualTo(AuthService.MAX_FAILED_ATTEMPTS);
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
        ArgumentCaptor<AuditEvent> cap = ArgumentCaptor.forClass(AuditEvent.class);
        verify(events).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(AuditEventType.LOGIN_FAILED_3_TIMES);
        assertThat(cap.getValue().getUser()).isSameAs(user);
    }

    @Test
    void lockedAccountRejectsEvenCorrectPassword() {
        User user = activeUser();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MIN));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.login(req(EMAIL, PASSWORD)))
                .isInstanceOf(AccountLockedException.class);
        verify(encoder, never()).matches(any(), any());
        verify(events, never()).save(any());
    }

    @Test
    void expiredLockIsClearedOnNextLogin() {
        User user = activeUser();
        user.setFailedLogins(AuthService.MAX_FAILED_ATTEMPTS);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches(PASSWORD, "hash")).thenReturn(true);
        when(jwt.generateToken(user)).thenReturn(TOKEN);
        LoginResponse res = authService.login(req(EMAIL, PASSWORD));
        assertThat(res.token()).isEqualTo(TOKEN);
        assertThat(user.getFailedLogins()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void disabledAccountCannotLogIn() {
        User user = activeUser();
        user.setActive(false);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(encoder.matches(PASSWORD, "hash")).thenReturn(true);
        assertThatThrownBy(() -> authService.login(req(EMAIL, PASSWORD)))
                .isInstanceOf(AccountDisabledException.class);
    }
}
