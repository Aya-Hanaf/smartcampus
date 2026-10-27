package ma.ensak.smartcampus.service;

import ma.ensak.smartcampus.dto.RoomFilterParams;
import ma.ensak.smartcampus.dto.RoomResponseDto;
import ma.ensak.smartcampus.model.Equipment;
import ma.ensak.smartcampus.model.Room;
import ma.ensak.smartcampus.model.RoomMapper;
import ma.ensak.smartcampus.model.RoomType;
import ma.ensak.smartcampus.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service de gestion des salles.
 * Il applique les filtres (capacité, type, équipement, bâtiment, mot-clé)
 * en combinant les résultats des repositories puis filtre en mémoire
 * pour l'aspect "équipement" (un salle doit posséder TOUS les équipements demandés).
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<RoomResponseDto> findAllWithFilters(RoomFilterParams filter) {
        List<Room> rooms = roomRepository.findAll();

        if (filter.capacity() != null) {
            int capacity = filter.capacity();
            rooms = rooms.stream()
                    .filter(r -> r.getCapacity() == capacity)
                    .collect(Collectors.toList());
        }

        if (filter.type() != null) {
            RoomType type = filter.type();
            rooms = rooms.stream()
                    .filter(r -> r.getType() == type)
                    .collect(Collectors.toList());
        }

        if (filter.buildingId() != null) {
            Long buildingId = filter.buildingId();
            rooms = rooms.stream()
                    .filter(r -> r.getBuilding().getId().equals(buildingId))
                    .collect(Collectors.toList());
        }

        List<String> equipmentNames = filter.equipment();
        if (equipmentNames != null && !equipmentNames.isEmpty()) {
            Set<String> requestedEquipment = new HashSet<>(equipmentNames);
            rooms = rooms.stream()
                    .filter(room -> {
                        Set<String> roomEquipment = room.getEquipment().stream()
                                .map(Equipment::getName)
                                .collect(Collectors.toSet());
                        return requestedEquipment.stream()
                                .allMatch(roomEquipment::contains);
                    })
                    .collect(Collectors.toList());
        }

        // Filtre: mot-clé (recherche dans le nom)
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            String keyword = filter.keyword().toLowerCase();
            rooms = rooms.stream()
                    .filter(r -> r.getName().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // Conversion DTO
        return RoomMapper.toDtoList(rooms);
    }
}
