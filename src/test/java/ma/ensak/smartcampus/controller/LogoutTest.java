package ma.ensak.smartcampus.controller;

import ma.ensak.smartcampus.config.JwtService;
import ma.ensak.smartcampus.model.RevokedToken;
import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.RevokedTokenRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class LogoutTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private RevokedTokenRepository revokedTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        revokedTokenRepository.deleteAll();
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User("etudiant.logout@ensak.ma",
                passwordEncoder.encode("MotDePasse-Test-123"), "Étudiant Logout", Role.STUDENT));
    }

    private ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @Test
    void logoutWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutReturns204() throws Exception {
        logout(jwtService.generateToken(user)).andExpect(status().isNoContent());
    }

    // Le cœur de SC-10 : après déconnexion, le jeton ne fonctionne plus, même s'il n'a pas expiré
    @Test
    void tokenIsRefusedAfterLogout() throws Exception {
        String token = jwtService.generateToken(user);
        me(token).andExpect(status().isOk());

        logout(token).andExpect(status().isNoContent());

        me(token).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutOnlyRevokesThatSession() throws Exception {
        String phone = jwtService.generateToken(user);
        String laptop = jwtService.generateToken(user);

        logout(phone).andExpect(status().isNoContent());

        me(laptop).andExpect(status().isOk());
    }

    @Test
    void secondLogoutWithSameTokenReturns401() throws Exception {
        String token = jwtService.generateToken(user);
        logout(token).andExpect(status().isNoContent());

        logout(token).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredEntriesAreCleanedUpOnLogout() throws Exception {
        revokedTokenRepository.save(new RevokedToken("ancien-jeton", Instant.now().minus(1, ChronoUnit.HOURS)));

        logout(jwtService.generateToken(user)).andExpect(status().isNoContent());

        assertThat(revokedTokenRepository.existsByJti("ancien-jeton")).isFalse();
        assertThat(revokedTokenRepository.count()).isEqualTo(1);
    }
}