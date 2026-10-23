package ma.ensak.smartcampus.controller;

import ma.ensak.smartcampus.config.JwtService;
import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de SC-33 : l'API est protégée par le jeton JWT (401 sans jeton valide). */
@SpringBootTest
@AutoConfigureMockMvc
class JwtSecurityTest {

    private static final String EMAIL = "etudiant.jwt@ensak.ma";
    private static final String UNAUTHORIZED_MESSAGE = "Vous devez être connecté pour accéder à cette ressource.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User(EMAIL, passwordEncoder.encode("MotDePasse-Test-123"),
                "Étudiant JWT", Role.STUDENT));
    }

    private ResultActions getMe(String authorizationHeader) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/auth/me");
        if (authorizationHeader != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return mockMvc.perform(request);
    }

    @Test
    void protectedRouteWithoutTokenReturns401WithClearMessage() throws Exception {
        getMe(null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(UNAUTHORIZED_MESSAGE));
    }

    @Test
    void validTokenGivesAccessAndIdentifiesTheUser() throws Exception {
        getMe("Bearer " + jwtService.generateToken(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.fullName").value("Étudiant JWT"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        getMe("Bearer ceci-n-est-pas-un-jeton").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherKeyReturns401() throws Exception {
        // jeton fabriqué avec une autre clé : signature invalide (tentative de falsification)
        JwtService attacker = new JwtService("", 60);
        getMe("Bearer " + attacker.generateToken(user)).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        String expired = jwtService.generateToken(user, Instant.now().minus(2, ChronoUnit.HOURS));
        getMe("Bearer " + expired).andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAccountWithValidTokenReturns401() throws Exception {
        String token = jwtService.generateToken(user);
        user.setActive(false);
        userRepository.save(user);

        getMe("Bearer " + token).andExpect(status().isUnauthorized());
    }
}