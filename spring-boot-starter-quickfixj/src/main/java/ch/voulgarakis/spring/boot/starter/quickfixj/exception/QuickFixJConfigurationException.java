package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class QuickFixJConfigurationException extends QuickFixJException {

    public QuickFixJConfigurationException(String message) {
        super(message);
    }

    public QuickFixJConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
