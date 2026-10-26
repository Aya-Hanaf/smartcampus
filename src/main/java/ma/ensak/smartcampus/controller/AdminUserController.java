package ma.ensak.smartcampus.controller;

import jakarta.validation.Valid;
import ma.ensak.smartcampus.dto.ChangeRoleRequest;
import ma.ensak.smartcampus.dto.CurrentUserResponse;
import ma.ensak.smartcampus.service.UserRoleService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRoleService userRoleService;

    public AdminUserController(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    /** SC-32 : attribuer un rôle (STUDENT, TEACHER ou ADMIN) à un utilisateur. */
    @PatchMapping("/{id}/role")
    public CurrentUserResponse changeRole(@PathVariable Long id,
                                          @Valid @RequestBody ChangeRoleRequest request,
                                          @AuthenticationPrincipal UserDetails admin) {
        return userRoleService.changeRole(id, request.role(), admin.getUsername());
    }
}