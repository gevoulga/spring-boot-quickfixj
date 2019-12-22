package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class QuickFixJConfigurationException extends QuickFixJException {

    private static final long serialVersionUID = 717598371784986098L;

    public QuickFixJConfigurationException(String message) {
        super(message);
    }

    public QuickFixJConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
