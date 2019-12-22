package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class QuickFixJSettingsNotFoundException extends QuickFixJException {

    private static final long serialVersionUID = -2135167058545104681L;

    public QuickFixJSettingsNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuickFixJSettingsNotFoundException(String message) {
        super(message);
    }
}
