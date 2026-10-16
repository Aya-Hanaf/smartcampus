package ma.ensak.smartcampus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Événement de sécurité (échecs de connexion, changements de rôle...). */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime at;

    @Column(length = 500)
    private String details;

    protected AuditEvent() {
        // requis par JPA
    }

    public AuditEvent(User user, String type, String details) {
        this.user = user;
        this.type = type;
        this.details = details;
        this.at = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getType() { return type; }
    public LocalDateTime getAt() { return at; }
    public String getDetails() { return details; }
}