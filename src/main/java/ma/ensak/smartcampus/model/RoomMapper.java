package ma.ensak.smartcampus.model;

import ma.ensak.smartcampus.dto.EquipmentDto;
import ma.ensak.smartcampus.dto.RoomResponseDto;
import ma.ensak.smartcampus.model.RoomType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe de conversion entre l'entité {@link Room} et le DTO de réponse {@link RoomResponseDto}.
 * Motif : découpler la couche service de la représentation de la réponse.
 */
public class RoomMapper {

    /** Convertit une entité Room en DTO. */
    public static RoomResponseDto toDto(Room room) {
        if (room == null) {
            return null;
        }
        return new RoomResponseDto(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getType(),
                room.getBuilding().getId(),
                room.getBuilding().getName(),
                toEquipmentDtos(room.getEquipment())
        );
    }

    /** Convertit une liste d'entités Room en liste de DTO. */
    public static List<RoomResponseDto> toDtoList(List<Room> rooms) {
        if (rooms == null) {
            return List.of();
        }
        return rooms.stream()
                .map(RoomMapper::toDto)
                .collect(Collectors.toList());
    }

    /** Convertit un DTO en entité (utile pour la création/modification). */
    public static Room toEntity(RoomResponseDto dto, Building building) {
        if (dto == null) {
            return null;
        }
        Room room = new Room(
                dto.name(),
                dto.capacity(),
                dto.type(),
                building
        );
        room.setId(dto.id());
        return room;
    }

    private static List<EquipmentDto> toEquipmentDtos(List<Equipment> equipmentList) {
        if (equipmentList == null || equipmentList.isEmpty()) {
            return List.of();
        }
        return equipmentList.stream()
                .map(eq -> new EquipmentDto(eq.getId(), eq.getName(), eq.getDescription()))
                .collect(Collectors.toList());
    }
}
