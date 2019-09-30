package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

public class ExceptionWrapper extends RuntimeException {

    private final Throwable ex;

    public ExceptionWrapper(Throwable cause) {
        super(cause);
        ex = cause;
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> T get() {
        return (T) ex;
    }
}
