package ma.ensak.smartcampus.dto;

/** DTO de réponse pour un matériel (équipement). */
public record EquipmentDto(
        Long id,
        String name,
        String description
) {
}
