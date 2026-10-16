package ma.ensak.smartcampus.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de SC-9 : un test par critère d'acceptation, plus les cas limites. */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String EMAIL = "etudiant.test@ensak.ma";
    private static final String PASSWORD = "MotDePasse-Test-123";
    private static final String WRONG_PASSWORD = "mauvais";
    private static final String BAD_CREDENTIALS_MESSAGE = "E-mail ou mot de passe incorrect.";

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

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // Critère 1 : identifiants corrects -> accès à l'application
    @Test
    void loginWithValidCredentialsReturnsJwt() throws Exception {
        login(EMAIL, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void emailIsNotCaseSensitive() throws Exception {
        login(EMAIL.toUpperCase(), PASSWORD).andExpect(status().isOk());
    }

    // Critère 2 : mot de passe incorrect -> message compréhensible
    @Test
    void wrongPasswordReturns401WithClearMessage() throws Exception {
        login(EMAIL, WRONG_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(BAD_CREDENTIALS_MESSAGE));
    }

    @Test
    void unknownEmailReturnsSameMessageAsWrongPassword() throws Exception {
        login("inconnu@ensak.ma", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(BAD_CREDENTIALS_MESSAGE));
    }

    // Critère 3 : trois échecs consécutifs -> événement de sécurité enregistré
    @Test
    void threeConsecutiveFailuresRecordAuditEvent() throws Exception {
        login(EMAIL, WRONG_PASSWORD);
        login(EMAIL, WRONG_PASSWORD);
        assertThat(auditEventRepository.findByUser_EmailAndType(EMAIL, AuditEventType.LOGIN_FAILED_3_TIMES))
                .isEmpty();

        login(EMAIL, WRONG_PASSWORD);
        assertThat(auditEventRepository.findByUser_EmailAndType(EMAIL, AuditEventType.LOGIN_FAILED_3_TIMES))
                .hasSize(1);
    }

    @Test
    void successfulLoginResetsFailureCounter() throws Exception {
        login(EMAIL, WRONG_PASSWORD);
        login(EMAIL, WRONG_PASSWORD);
        login(EMAIL, PASSWORD).andExpect(status().isOk());

        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(user.getFailedLogins()).isZero();
    }

    @Test
    void disabledAccountCannotLogIn() throws Exception {
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        user.setActive(false);
        userRepository.save(user);

        login(EMAIL, PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        login("", "").andExpect(status().isBadRequest());
    }

    @Test
    void passwordIsStoredHashedWithBcrypt() {
        User user = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    // Non-régression : Jenkins utilise /api/health, qui doit rester public
    @Test
    void healthEndpointStaysPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}