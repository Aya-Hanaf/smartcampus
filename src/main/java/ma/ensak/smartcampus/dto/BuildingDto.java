package ma.ensak.smartcampus.dto;

/** DTO de réponse pour un bâtiment. */
public record BuildingDto(
        Long id,
        String name,
        String code,
        String address
) {
}
