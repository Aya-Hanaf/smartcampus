package ma.ensak.smartcampus.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Utilisateur introuvable.");
    }
}