package ma.ensak.smartcampus.dto;

import ma.ensak.smartcampus.model.Role;

public record CurrentUserResponse(String email, String fullName, Role role) {
}