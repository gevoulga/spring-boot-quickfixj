package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class QuickFixJSettingsNotFoundException extends QuickFixJException {

    public QuickFixJSettingsNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuickFixJSettingsNotFoundException(String message) {
        super(message);
    }
}
