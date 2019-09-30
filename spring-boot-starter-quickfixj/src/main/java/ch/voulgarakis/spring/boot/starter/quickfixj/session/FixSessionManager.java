package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.ExceptionWrapper;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.*;

public class FixSessionManager implements Application {

    private static final Logger LOG = LoggerFactory.getLogger(FixSessionManager.class);
    private final Map<SessionID, FixSession> fixSessions;

    public FixSessionManager(SessionSettings sessionSettings, List<FixSession> sessions) {
        if (sessions.size() > 1) {
            List<String> sessionNames = sessions.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            ensureUniqueSessionNames(sessionNames,
                    "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session name.");
        }
        fixSessions = stream(sessionSettings)
                .map(sessionID -> getFixSession(sessionSettings, sessions, sessionID))
                .collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight));
    }

    private FixSession retrieveSession(SessionID sessionId) {
        FixSession fixSession = fixSessions.get(sessionId);
        if (Objects.isNull(fixSession)) {
            throw new QuickFixJConfigurationException(
                    String.format("No FixSession receiver for session [%s] ", sessionId));
        }
        return fixSession;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        retrieveSession(sessionId);
        LOG.debug("Session created: {}", sessionId);
    }

    @Override
    public void onLogon(SessionID sessionId) {
        LOG.debug("Session logged on: {}", sessionId);
    }

    @Override
    public void onLogout(SessionID sessionId) {
        LOG.debug("Session logged out: {}", sessionId);
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        try {
            if (isMessageOfType(message, MsgType.LOGON)) { // || isMessageOfType(message, MsgType.LOGOUT)) {
                try {
                    retrieveSession(sessionId).authenticate(message);
                } catch (RejectLogon rejectLogon) {
                    LOG.warn("Failed to authenticate message type. Session: {}, Message: {}", sessionId, message,
                            rejectLogon);
                }
            }
            LOG.debug("Sending administrative message. Session: {}, Message: {}", sessionId, message);
        } catch (FieldNotFound fieldNotFound) {
            LOG.warn("Failed to extract message type. Session: {}, Message: {}", sessionId, message, fieldNotFound);
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        try {
            if (isMessageOfType(message, MsgType.LOGON)) {
                retrieveSession(sessionId).authenticate(message);
            } else if (isMessageOfType(message, MsgType.LOGOUT) || RejectException.isReject(message)) {
                retrieveSession(sessionId).error(new RejectException(message));
            } else if (isMessageOfType(message, MsgType.HEARTBEAT, MsgType.RESEND_REQUEST)) {
                retrieveSession(sessionId).received(message);
            }
            LOG.debug("Received administrative message. Session: {}, Message: {}", sessionId, message);
        } catch (Throwable e) {
            LOG.error("Failed to process FIX message: Session {}, Message: {}", sessionId, message, e);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        LOG.debug("Sending message. Session: {}, Message: {}", sessionId, message);
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        try {
            if (RejectException.isReject(message)) {
                retrieveSession(sessionId).error(new RejectException(message));
            } else {
                retrieveSession(sessionId).received(message);
            }
            LOG.debug("Received message. Session: {}, Message: {}", sessionId, message);
        } catch (Throwable e) {
            LOG.error("Failed to process FIX message: Session {}, Message: {}", sessionId, message, e);
        }
    }

    public static boolean isMessageOfType(Message message, String... types) throws FieldNotFound {
        try {
            return Stream.of(types).anyMatch(type -> {
                try {
                    return type.equals(message.getHeader().getField(new MsgType()).getValue());
                } catch (FieldNotFound fieldNotFound) {
                    throw new ExceptionWrapper(fieldNotFound);
                }
            });
        } catch (ExceptionWrapper e) {
            throw e.<FieldNotFound>get();
        }
    }
}
