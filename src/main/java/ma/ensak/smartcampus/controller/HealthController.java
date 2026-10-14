package ma.ensak.smartcampus.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point d'entrée de contrôle : permet de vérifier que l'application tourne
 * (utilisé par Jenkins, Docker et Kubernetes dans la suite des TP).
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.version:dev}")
    private String version;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", applicationName,
                "version", version,
                "timestamp", Instant.now().toString());
    }
}
