package ma.ensak.smartcampus.controller;

import ma.ensak.smartcampus.dto.RoomFilterParams;
import ma.ensak.smartcampus.dto.RoomResponseDto;
import ma.ensak.smartcampus.model.RoomType;
import ma.ensak.smartcampus.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API REST des salles.
 * GET /api/rooms : recherche des salles avec filtres optionnels.
 *
 * <p>Exemples de requêtes :</p>
 * <pre>
 *   GET /api/rooms
 *   GET /api/rooms?capacity=30
 *   GET /api/rooms?type=LABORATORY
 *   GET /api/rooms?building=1
 *   GET /api/rooms?equipment=projector,projector2
 *   GET /api/rooms?keyword=TD
 * </pre>
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomResponseDto> searchRooms(
            @RequestParam(required = false) Integer capacity,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long building,
            @RequestParam(required = false) List<String> equipment,
            @RequestParam(required = false) String keyword
    ) {
        RoomType roomType = parseRoomType(type);
        return roomService.findAllWithFilters(
                new RoomFilterParams(capacity, roomType, building, equipment, keyword)
        );
    }

    /**
     * Convertit la chaîne de requête en enum RoomType.
     * Accepte les noms en majuscules ou minuscules (ex. "LABORATORY", "laboratory", "laboratoire").
     */
    private RoomType parseRoomType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return RoomType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // Tentative de correspondance avec un alias français
            String lower = type.trim().toLowerCase();
            if ("td".equals(lower) || "travaux dirigés".equals(lower) || "travauxdirigés".equals(lower)) {
                return RoomType.TD;
            }
            if ("salle de travail".equals(lower) || "work".equals(lower)) {
                return RoomType.WORK;
            }
            if ("laboratoire".equals(lower) || "lab".equals(lower) || "laboratory".equals(lower)) {
                return RoomType.LABORATORY;
            }
            if ("amphithéâtre".equals(lower) || "amphi".equals(lower) || "amphitheater".equals(lower)) {
                return RoomType.AMPHITHEATER;
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Type de salle invalide : '" + type + "'. Valeurs attendues : TD, WORK, LABORATORY, AMPHITHEATER."
            );
        }
    }
}

