package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

import quickfix.Message;
import quickfix.field.Text;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetField;
import static java.lang.String.format;

public class SessionDroppedException extends SessionException {

    private static final long serialVersionUID = -6938947691835025139L;

    public SessionDroppedException(Message fixMessage) {
        super(fixMessage, extractText(fixMessage));
    }

    public SessionDroppedException() {
        this(null);
    }

    private static String extractText(Message message) {
        return "Logged Out" +
                safeGetField(message, new Text()).map(s -> format(": Text: %s", s)).orElse(null);
    }
}
