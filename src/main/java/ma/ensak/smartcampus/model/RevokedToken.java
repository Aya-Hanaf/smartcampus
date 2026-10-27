package ma.ensak.smartcampus.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant unique du jeton (claim "jti"). */
    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    /** Après cette date, le jeton est expiré de toute façon : l'entrée peut être supprimée. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
        // requis par JPA
    }

    public RevokedToken(String jti, Instant expiresAt) {
        this.jti = jti;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getJti() { return jti; }
    public Instant getExpiresAt() { return expiresAt; }
}