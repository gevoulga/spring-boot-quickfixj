package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionNotFound;

public abstract class DefaultFixSession extends AbstractFixSession implements FixSession {

    @Override
    public Message send(Message message) {
        try {
            Session.sendToTarget(message, getSessionId());
            return message;
        } catch (SessionNotFound sessionNotFound) {
            throw new QuickFixJException(sessionNotFound);
        }
    }
}
