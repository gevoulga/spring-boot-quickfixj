package ch.voulgarakis.spring.boot.starter.quickfixj.session;


import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.isMessageOfType;

public class FixSessionManager implements Application {

    private static final Logger LOG = LoggerFactory.getLogger(FixSessionManager.class);
    private final Map<SessionID, AbstractFixSession> fixSessions;
    private final StartupLatch startupLatch;

    public FixSessionManager(SessionSettings sessionSettings, List<AbstractFixSession> sessions, StartupLatch startupLatch) {
        if (sessions.size() > 1) {
            List<String> sessionNames = sessions.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            FixSessionUtils.ensureUniqueSessionNames(sessionNames, "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session name.");
        }
        fixSessions = FixSessionUtils.stream(sessionSettings)
                .map(sessionID -> FixSessionUtils.getFixSession(sessionSettings, sessions, sessionID))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
        this.startupLatch = startupLatch;
    }

    private AbstractFixSession retrieveSession(SessionID sessionId) {
        AbstractFixSession fixSession = fixSessions.get(sessionId);
        if (Objects.isNull(fixSession)) {
            throw new QuickFixJConfigurationException(String.format("No AbstractFixSession receiver for session [%s] ", sessionId));
        }
        return fixSession;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        LOG.debug("Session created: {}", sessionId);
        startupLatch.created(sessionId);
        retrieveSession(sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        LOG.debug("Session logged on: {}", sessionId);
        startupLatch.loggedOn(sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        LOG.debug("Session logged out: {}", sessionId);
        retrieveSession(sessionId).error(new SessionDroppedException());
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        if (!isMessageOfType(message, MsgType.HEARTBEAT, MsgType.RESEND_REQUEST)) {
            if (isMessageOfType(message, MsgType.LOGON)) { // || isMessageOfType(message, MsgType.LOGOUT)) {
                LOG.debug("Sending login message. Session: {}, Message: {}", sessionId, message);
                try {
                    retrieveSession(sessionId).authenticate(message);
                } catch (RejectLogon rejectLogon) {
                    LOG.warn("Failed to authenticate message type. Session: {}, Message: {}", sessionId, message,
                            rejectLogon);
                }
            } else {
                LOG.debug("Sending administrative message. Session: {}, Message: {}", sessionId, message);
            }
            // retrieveSession(sessionId).sent(message);
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        try {
            //Heartbeat & Resend are omitted
            if (!isMessageOfType(message, MsgType.HEARTBEAT, MsgType.RESEND_REQUEST)) {
                LOG.debug("Received administrative message. Session: {}, Message: {}", sessionId, message);
                if (isMessageOfType(message, MsgType.LOGON)) {
                    retrieveSession(sessionId).authenticate(message);
                } else if (isMessageOfType(message, MsgType.LOGOUT)) {
                    retrieveSession(sessionId).error(new SessionDroppedException(message));
                } else if (RejectException.isReject(message)) {
                    retrieveSession(sessionId).error(new RejectException(message));
                }
            }
        } catch (Throwable e) {
            LOG.error("Failed to process FIX message: Session {}, Message: {}", sessionId, message, e);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        LOG.info("Sending message. Session: {}, Message: {}", sessionId, message);
        // retrieveSession(sessionId).sent(message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        try {
            LOG.info("Received message. Session: {}, Message: {}", sessionId, message);
            if (RejectException.isReject(message)) {
                retrieveSession(sessionId).error(new RejectException(message));
            } else {
                retrieveSession(sessionId).received(message);
            }
        } catch (Throwable e) {
            LOG.error("Failed to process FIX message: Session {}, Message: {}", sessionId, message, e);
        }
    }
}
