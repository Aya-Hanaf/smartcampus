package ma.ensak.smartcampus.repository;

import ma.ensak.smartcampus.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
