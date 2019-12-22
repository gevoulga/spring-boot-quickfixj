package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class QuickFixJException extends RuntimeException {

    private static final long serialVersionUID = 8859671565748911596L;

    public QuickFixJException(String message) {
        super(message);
    }

    public QuickFixJException(Throwable cause) {
        super(cause);
    }

    public QuickFixJException(String message, Throwable cause) {
        super(message, cause);
    }
}
