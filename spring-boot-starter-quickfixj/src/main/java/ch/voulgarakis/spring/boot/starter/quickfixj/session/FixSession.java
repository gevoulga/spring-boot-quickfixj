package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import quickfix.*;

import java.util.Objects;

public abstract class FixSession {

    private SessionID sessionId;

    /**
     * Notifies that a message has been received.
     *
     * @param message the message that has been received.
     */
    protected abstract void received(Message message);

    /**
     * Notifies that a reject message has been received of type:
     * REJECT, BUSINESS_MESSAGE_REJECT, ORDER_CANCEL_REJECT, MARKET_DATA_REQUEST_REJECT, QUOTE_REQUEST_REJECT
     *
     * @param message the actual reject message that has been received.
     */
    protected abstract void error(RejectException message);


    public void send(Message message) {
        try {
            Session.sendToTarget(message, sessionId);
        } catch (SessionNotFound sessionNotFound) {
            throw new QuickFixJException(sessionNotFound);
        }
    }

    /**
     * Handle authentication for this session.
     * If session is an Initiator, this should set the username & password of the session, on the Logon
     * If session is an acceptor, this should authenticate the client based on the Logon Message.
     *
     * @param message in case of initiator: message where username/password should be set.
     *                in case of accepter: the message with the credentials that should be authenticated.
     * @throws RejectLogon in case of authentication failure.
     */
    protected abstract void authenticate(Message message) throws RejectLogon;

    final void setSessionId(SessionID sessionID) {
        if (Objects.isNull(this.sessionId)) {
            this.sessionId = sessionID;
        } else {
            throw new QuickFixJConfigurationException("Not allowed to set SessionId more than once.");
        }
    }
}
