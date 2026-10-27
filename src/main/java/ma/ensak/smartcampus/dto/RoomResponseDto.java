package ma.ensak.smartcampus.dto;

import ma.ensak.smartcampus.model.RoomType;

import java.util.List;

/** DTO de réponse pour une salle. */
public record RoomResponseDto(
        Long id,
        String name,
        int capacity,
        RoomType type,
        Long buildingId,
        String buildingName,
        List<EquipmentDto> equipment
) {
}
