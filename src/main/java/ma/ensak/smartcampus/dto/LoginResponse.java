package ma.ensak.smartcampus.dto;

import ma.ensak.smartcampus.model.Role;

/** Réponse renvoyée au frontend après une connexion réussie. */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String email,
        String fullName,
        Role role
) {
}