package ma.ensak.smartcampus.controller;

import jakarta.validation.Valid;
import ma.ensak.smartcampus.dto.CurrentUserResponse;
import ma.ensak.smartcampus.dto.LoginRequest;
import ma.ensak.smartcampus.dto.LoginResponse;
import ma.ensak.smartcampus.service.AuthService;
import ma.ensak.smartcampus.service.TokenRevocationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final TokenRevocationService tokenRevocationService;

    public AuthController(AuthService authService, TokenRevocationService tokenRevocationService) {
        this.authService = authService;
        this.tokenRevocationService = tokenRevocationService;
    }

    /** SC-9 : connexion avec e-mail et mot de passe, renvoie un JWT. */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** SC-33 : utilisateur connecté (route protégée : jeton JWT obligatoire). */
    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return authService.currentUser(principal.getUsername());
    }

    /**
     * SC-10 : déconnexion sécurisée. Le jeton est révoqué côté serveur :
     * il est refusé (401) même s'il n'a pas encore expiré.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        // le filtre a déjà vérifié que l'en-tête commence par "Bearer " et que le jeton est valide
        tokenRevocationService.revoke(authorization.substring(BEARER_PREFIX.length()).trim());
    }
}