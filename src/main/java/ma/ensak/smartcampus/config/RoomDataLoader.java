package ma.ensak.smartcampus.config;

import ma.ensak.smartcampus.model.Building;
import ma.ensak.smartcampus.model.Equipment;
import ma.ensak.smartcampus.model.Room;
import ma.ensak.smartcampus.model.RoomType;
import ma.ensak.smartcampus.repository.BuildingRepository;
import ma.ensak.smartcampus.repository.EquipmentRepository;
import ma.ensak.smartcampus.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Chargeur de données de démonstration pour les salles et bâtiments.
 * Idempotent : ne fait rien si des données existent déjà.
 */
@Component
public class RoomDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RoomDataLoader.class);

    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;

    public RoomDataLoader(BuildingRepository buildingRepository,
                          RoomRepository roomRepository,
                          EquipmentRepository equipmentRepository) {
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (buildingRepository.count() > 0) {
            log.info("Données de démonstration déjà présentes, ignoré.");
            return;
        }

        log.info("Chargement des données de démonstration...");

        // ── Bâtiments ───────────────────────────────────────────────
        Building b1 = new Building("Bâtiment A", "B001", "12 rue de l'ENSAM, Khouribga");
        Building b2 = new Building("Bâtiment B", "B002", "15 avenue de l'Université, Khouribga");
        buildingRepository.save(b1);
        buildingRepository.save(b2);

        // ── Équipements ─────────────────────────────────────────────
        Equipment projector = new Equipment("Projecteur", "Projecteur HD 10000 lumens");
        Equipment computer = new Equipment("Ordinateur portable", "Portable usable");
        Equipment microscope = new Equipment("Microscope", "Microscope optique bins");
        Equipment whiteboard = new Equipment("Tableau blanc", "Tableau blanc magnétique");
        equipmentRepository.save(projector);
        equipmentRepository.save(computer);
        equipmentRepository.save(microscope);
        equipmentRepository.save(whiteboard);

        // ── Salles ──────────────────────────────────────────────────
        Room r1 = new Room("TD 1", 30, RoomType.TD, b1);
        r1.getEquipment().add(projector);
        r1.getEquipment().add(whiteboard);

        Room r2 = new Room("TD 2", 30, RoomType.TD, b1);
        r2.getEquipment().add(whiteboard);

        Room r3 = new Room("Laboratoire 1", 20, RoomType.LABORATORY, b1);
        r3.getEquipment().add(projector);
        r3.getEquipment().add(computer);
        r3.getEquipment().add(whiteboard);

        Room r4 = new Room("Amphithéâtre 1", 200, RoomType.AMPHITHEATER, b2);

        roomRepository.save(r1);
        roomRepository.save(r2);
        roomRepository.save(r3);
        roomRepository.save(r4);

        log.info("Données de démonstration chargées : 2 bâtiments, 4 salles, 4 équipements.");
    }
}
