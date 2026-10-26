package ma.ensak.smartcampus.dto;

import jakarta.validation.constraints.NotNull;
import ma.ensak.smartcampus.model.Role;

public record ChangeRoleRequest(@NotNull Role role) {
}