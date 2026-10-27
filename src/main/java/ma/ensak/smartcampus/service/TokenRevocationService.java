package ma.ensak.smartcampus.service;

import io.jsonwebtoken.Claims;
import ma.ensak.smartcampus.config.JwtService;
import ma.ensak.smartcampus.model.RevokedToken;
import ma.ensak.smartcampus.repository.RevokedTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenRevocationService {

    private final JwtService jwtService;
    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationService(JwtService jwtService, RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    /** Révoque le jeton : il sera refusé (401) jusqu'à son expiration. */
    @Transactional
    public void revoke(String token) {
        Claims claims = jwtService.parse(token);

        // ménage : les jetons déjà expirés n'ont plus besoin d'être dans la liste
        revokedTokenRepository.deleteExpired(Instant.now());

        String jti = claims.getId();
        if (jti == null || revokedTokenRepository.existsByJti(jti)) {
            return;
        }
        revokedTokenRepository.save(new RevokedToken(jti, claims.getExpiration().toInstant()));
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String jti) {
        return jti != null && revokedTokenRepository.existsByJti(jti);
    }
}