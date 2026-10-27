package ma.ensak.smartcampus.dto;

import ma.ensak.smartcampus.model.RoomType;

import java.util.List;

/**
 * Paramètres de filtre pour la recherche des salles.
 * Tous les champs sont optionnels ; s'ils sont null/absents, le filtre correspondant n'est pas appliqué.
 */
public record RoomFilterParams(
        Integer capacity,
        RoomType type,
        Long buildingId,
        List<String> equipment,
        String keyword
) {
}
