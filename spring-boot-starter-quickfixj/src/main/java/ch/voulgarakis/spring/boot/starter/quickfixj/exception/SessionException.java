package ch.voulgarakis.spring.boot.starter.quickfixj.exception;

import quickfix.Message;

public abstract class SessionException extends Exception {

    private static final long serialVersionUID = -629107559772916457L;
    private final Message fixMessage;

    public SessionException(Message fixMessage, String message) {
        super(message);
        this.fixMessage = fixMessage;
    }

    public Message getFixMessage() {
        return fixMessage;
    }
}
