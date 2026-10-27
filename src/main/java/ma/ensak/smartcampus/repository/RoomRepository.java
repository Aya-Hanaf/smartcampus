package ma.ensak.smartcampus.repository;

import ma.ensak.smartcampus.model.Room;
import ma.ensak.smartcampus.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** Filtres simples (utilisés par le service pour combiner). */
    List<Room> findByCapacity(int capacity);
    List<Room> findByType(RoomType type);
    List<Room> findByBuildingId(Long buildingId);

    /** Recherche par nom (pour le filtre de nom, si besoin). */
    List<Room> findByNameContainingIgnoreCase(String keyword);
}
