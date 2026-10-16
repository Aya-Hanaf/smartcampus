package ma.ensak.smartcampus.dto;

/** Format commun des erreurs renvoyées par l'API. */
public record ErrorResponse(int status, String message) {
}