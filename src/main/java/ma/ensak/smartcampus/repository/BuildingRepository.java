package ma.ensak.smartcampus.repository;

import ma.ensak.smartcampus.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, Long> {
}
