package ma.ensak.smartcampus.service;

import ma.ensak.smartcampus.dto.CurrentUserResponse;
import ma.ensak.smartcampus.exception.InvalidRoleChangeException;
import ma.ensak.smartcampus.exception.UserNotFoundException;
import ma.ensak.smartcampus.model.AuditEvent;
import ma.ensak.smartcampus.model.AuditEventType;
import ma.ensak.smartcampus.model.Role;
import ma.ensak.smartcampus.model.User;
import ma.ensak.smartcampus.repository.AuditEventRepository;
import ma.ensak.smartcampus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRoleService {

    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;

    public UserRoleService(UserRepository userRepository, AuditEventRepository auditEventRepository) {
        this.userRepository = userRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public CurrentUserResponse changeRole(Long userId, Role newRole, String adminEmail) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // un admin qui se retire ses propres droits pourrait bloquer l'administration
        if (user.getEmail().equalsIgnoreCase(adminEmail)) {
            throw new InvalidRoleChangeException("Vous ne pouvez pas modifier votre propre rôle.");
        }

        Role oldRole = user.getRole();
        if (oldRole != newRole) {
            user.setRole(newRole);
            userRepository.save(user);
            auditEventRepository.save(new AuditEvent(
                    user,
                    AuditEventType.ROLE_CHANGED,
                    "Rôle " + oldRole + " -> " + newRole + " par " + adminEmail));
        }

        return new CurrentUserResponse(user.getEmail(), user.getFullName(), user.getRole());
    }
}