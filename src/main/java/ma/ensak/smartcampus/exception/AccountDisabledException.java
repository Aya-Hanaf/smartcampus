package ma.ensak.smartcampus.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("Ce compte est désactivé. Contactez l'administration.");
    }
}