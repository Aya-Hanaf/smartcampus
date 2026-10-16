package ma.ensak.smartcampus.repository;

import ma.ensak.smartcampus.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByUser_EmailAndType(String email, String type);
}