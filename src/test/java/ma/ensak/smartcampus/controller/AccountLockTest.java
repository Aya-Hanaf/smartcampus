package ma.ensak.smartcampus.controller;

import ma.ensak.smartcampus.exception.AccountLockedException;
import ma.ensak.smartcampus.model.AuditEventType;
import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests du bug SC-13 : message et journalisation après trois échecs de connexion. */
@SpringBootTest
@AutoConfigureMockMvc
class AccountLockTest {

    private static final String EMAIL = "etudiant.lock@ensak.ma";
    private static final String PASSWORD = "MotDePasse-Test-123";
    private static final String WRONG_PASSWORD = "mauvais";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), "Étudiant Test", Role.STUDENT));
    }

    private ResultActions login(String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(EMAIL, password);
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void lockAccount() throws Exception {
        login(WRONG_PASSWORD);
        login(WRONG_PASSWORD);
        login(WRONG_PASSWORD);
    }

    // Bug SC-13, partie 1 : le message après 3 échecs
    @Test
    void thirdFailureShowsLockedMessageInsteadOfUnknownError() throws Exception {
        login(WRONG_PASSWORD).andExpect(status().isUnauthorized());
        login(WRONG_PASSWORD).andExpect(status().isUnauthorized());

        login(WRONG_PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(AccountLockedException.MESSAGE));
    }

    // Bug SC-13, partie 2 : l'événement est enregistré dans les logs de sécurité
    @Test
    void thirdFailureIsRecordedInSecurityLog() throws Exception {
        lockAccount();

        assertThat(auditEventRepository.findByUser_EmailAndType(EMAIL, AuditEventType.LOGIN_FAILED_3_TIMES))
                .hasSize(1);

        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(user.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void lockedAccountRejectsEvenCorrectPassword() throws Exception {
        lockAccount();

        login(PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(AccountLockedException.MESSAGE));
    }

    @Test
    void attemptsWhileLockedDoNotCreateNewEvents() throws Exception {
        lockAccount();
        login(WRONG_PASSWORD);
        login(WRONG_PASSWORD);

        assertThat(auditEventRepository.findByUser_EmailAndType(EMAIL, AuditEventType.LOGIN_FAILED_3_TIMES))
                .hasSize(1);
    }

    @Test
    void accountUnlocksAfterLockDuration() throws Exception {
        lockAccount();

        // on simule la fin du délai de verrouillage
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        userRepository.save(user);

        login(PASSWORD).andExpect(status().isOk());

        User unlocked = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(unlocked.getFailedLogins()).isZero();
        assertThat(unlocked.getLockedUntil()).isNull();
    }
}