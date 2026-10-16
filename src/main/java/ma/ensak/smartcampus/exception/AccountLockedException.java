package ma.ensak.smartcampus.exception;

public class AccountLockedException extends RuntimeException {

    public static final String MESSAGE = "Compte temporairement verrouillé. Réessayez dans quelques minutes.";

    public AccountLockedException() {
        super(MESSAGE);
    }
}