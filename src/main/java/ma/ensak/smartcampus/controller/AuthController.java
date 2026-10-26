package ma.ensak.smartcampus.controller;

import jakarta.validation.Valid;
import ma.ensak.smartcampus.dto.CurrentUserResponse;
import ma.ensak.smartcampus.dto.LoginRequest;
import ma.ensak.smartcampus.dto.LoginResponse;
import ma.ensak.smartcampus.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal UserDetails principal) {
        return authService.currentUser(principal.getUsername());
    }
}