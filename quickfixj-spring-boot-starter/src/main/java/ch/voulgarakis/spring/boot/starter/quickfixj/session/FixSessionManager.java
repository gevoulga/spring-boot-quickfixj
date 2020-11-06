/*
 * Copyright (c) 2020 Georgios Voulgarakis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.voulgarakis.spring.boot.starter.quickfixj.session;


import ch.voulgarakis.spring.boot.starter.quickfixj.authentication.AuthenticationService;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingContext;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingId;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Application;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.field.MsgType;

import java.util.Objects;
import java.util.Optional;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.isMessageOfType;

public class FixSessionManager implements Application {

    private static final Logger LOG = LoggerFactory.getLogger(FixSessionManager.class);
    private final InternalFixSessions<? extends AbstractFixSession> fixSessions;
    private final FixConnectionType fixConnectionType;
    private final StartupLatch startupLatch;
    private final LoggingId loggingId;
    private final AuthenticationService authenticationService;

    public FixSessionManager(InternalFixSessions<? extends AbstractFixSession> fixSessions,
            FixConnectionType fixConnectionType,
            StartupLatch startupLatch, LoggingId loggingId,
            AuthenticationService authenticationService) {
        this.fixSessions = fixSessions;
        this.fixConnectionType = fixConnectionType;
        this.startupLatch = startupLatch;
        this.loggingId = loggingId;
        this.authenticationService = authenticationService;
    }

    private Logger logger(SessionID sessionId) {
        if (Objects.isNull(sessionId)) {
            return LOG;
        }
        return Optional.ofNullable(fixSessions.retrieveSession(sessionId))
                .map(AbstractFixSession::getClass)
                .map(LoggerFactory::getLogger)
                .orElseGet(() -> LOG);
    }

    @Override
    public void onCreate(SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            logger(sessionId).info("Session created.");
            startupLatch.created(sessionId);
            fixSessions.retrieveSession(sessionId);
        }
    }

    @Override
    public void onLogon(SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            logger(sessionId).info("Session logged on.");
            startupLatch.loggedOn(sessionId);
            fixSessions.retrieveSession(sessionId).loggedOn();
        }
    }

    @Override
    public void onLogout(SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            if (fixConnectionType.isAcceptor()) {
                logger(sessionId).info("Session logged out.");
            } else {
                logger(sessionId).error("Session logged out.");
            }
            fixSessions.retrieveSession(sessionId).error(new SessionDroppedException());
        }
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            if (!isMessageOfType(message, MsgType.HEARTBEAT, MsgType.RESEND_REQUEST)) {
                Logger logger = logger(sessionId);
                if (isMessageOfType(message, MsgType.LOGON)) { // || isMessageOfType(message, MsgType.LOGOUT)) {
                    logger.info("Sending login message: {}", message);
                    try {
                        authenticationService.authenticate(sessionId, message);
                    } catch (RejectLogon rejectLogon) {
                        logger.error("Failed to authenticate message type: {}", message,
                                rejectLogon);
                    }
                } else {
                    LOG.debug("Sending administrative message: {}", message);
                }
                // retrieveSession(sessionId).sent(message);
            }
        }
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            //Heartbeat & Resend are omitted
            if (!isMessageOfType(message, MsgType.HEARTBEAT, MsgType.RESEND_REQUEST)) {
                logger(sessionId).debug("Received administrative message: {}", message);
                if (isMessageOfType(message, MsgType.LOGON)) {
                    AbstractFixSession fixSession = fixSessions.retrieveSession(sessionId);
                    try {
                        authenticationService.authenticate(sessionId, message);
                        fixSession.loggedOn();
                    } catch (RejectLogon rejectLogon) {
                        logger(sessionId).error("Failed to authenticate message type: {}", message,
                                rejectLogon);
                    }
                } else if (isMessageOfType(message, MsgType.LOGOUT)) {
                    fixSessions.retrieveSession(sessionId).error(new SessionDroppedException(message));
                } else if (RejectException.isReject(message)) {
                    fixSessions.retrieveSession(sessionId).error(new RejectException(message));
                }
            }
        } catch (Throwable e) {
            logger(sessionId).error("Failed to process FIX message: {}", message, e);
        }
    }

    @Override
    public void toApp(Message message, SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            logger(sessionId).info("Sending message: {}", message);
            // retrieveSession(sessionId).sent(message);
        }
    }

    @Override
    public void fromApp(Message message, SessionID sessionId) {
        try (LoggingContext ignore = loggingId.loggingCtx(sessionId)) {
            logger(sessionId).info("Received message: {}", message);
            if (RejectException.isReject(message)) {
                fixSessions.retrieveSession(sessionId).error(new RejectException(message));
            } else {
                fixSessions.retrieveSession(sessionId).received(message);
            }
        } catch (Throwable e) {
            logger(sessionId).error("Failed to process FIX message: {}", message, e);
        }
    }
}
