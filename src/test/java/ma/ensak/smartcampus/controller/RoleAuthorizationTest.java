package ma.ensak.smartcampus.controller;

import ma.ensak.smartcampus.config.JwtService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests de SC-32 : rôles attribués par l'admin, routes admin réservées (401 / 403). */
@SpringBootTest
@AutoConfigureMockMvc
class RoleAuthorizationTest {

    private static final String FORBIDDEN_MESSAGE = "Vous n'avez pas les droits nécessaires pour cette action.";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditEventRepository auditEventRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private User student;
    private User teacher;
    private User admin;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        userRepository.deleteAll();
        String hash = passwordEncoder.encode("MotDePasse-Test-123");
        student = userRepository.save(new User("etudiant.role@ensak.ma", hash, "Étudiant", Role.STUDENT));
        teacher = userRepository.save(new User("enseignant.role@ensak.ma", hash, "Enseignant", Role.TEACHER));
        admin = userRepository.save(new User("admin.role@ensak.ma", hash, "Admin", Role.ADMIN));
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    private ResultActions changeRole(String authorizationHeader, Long userId, String role) throws Exception {
        MockHttpServletRequestBuilder request = patch("/api/admin/users/{id}/role", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role": "%s"}
                        """.formatted(role));
        if (authorizationHeader != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return mockMvc.perform(request);
    }

    // Exigence du document : 401 sans jeton
    @Test
    void adminRouteWithoutTokenReturns401() throws Exception {
        changeRole(null, student.getId(), "TEACHER").andExpect(status().isUnauthorized());
    }

    // Exigence du document : 403 avec un mauvais rôle
    @Test
    void studentCannotAccessAdminRoutes() throws Exception {
        changeRole(tokenOf(student), teacher.getId(), "ADMIN")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(FORBIDDEN_MESSAGE));
    }

    @Test
    void teacherCannotAccessAdminRoutes() throws Exception {
        changeRole(tokenOf(teacher), student.getId(), "TEACHER").andExpect(status().isForbidden());
    }

    @Test
    void adminCanChangeRole() throws Exception {
        changeRole(tokenOf(admin), student.getId(), "TEACHER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("etudiant.role@ensak.ma"))
                .andExpect(jsonPath("$.role").value("TEACHER"));

        assertThat(userRepository.findById(student.getId()).orElseThrow().getRole()).isEqualTo(Role.TEACHER);
    }

    @Test
    void roleChangeIsRecordedInSecurityLog() throws Exception {
        changeRole(tokenOf(admin), student.getId(), "TEACHER").andExpect(status().isOk());

        assertThat(auditEventRepository.findByUser_EmailAndType("etudiant.role@ensak.ma", AuditEventType.ROLE_CHANGED))
                .hasSize(1);
    }

    @Test
    void newRoleTakesEffectWithoutReconnecting() throws Exception {
        String studentToken = tokenOf(student);   // jeton émis quand il était encore STUDENT

        changeRole(tokenOf(admin), student.getId(), "ADMIN").andExpect(status().isOk());

        // le rôle est relu en base à chaque requête : le même jeton donne maintenant accès à l'admin
        changeRole(studentToken, teacher.getId(), "STUDENT").andExpect(status().isOk());
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        changeRole(tokenOf(admin), admin.getId(), "STUDENT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vous ne pouvez pas modifier votre propre rôle."));
    }

    @Test
    void unknownUserReturns404() throws Exception {
        changeRole(tokenOf(admin), 999_999L, "TEACHER").andExpect(status().isNotFound());
    }

    @Test
    void invalidRoleReturns400() throws Exception {
        changeRole(tokenOf(admin), student.getId(), "SUPERADMIN").andExpect(status().isBadRequest());
    }
}