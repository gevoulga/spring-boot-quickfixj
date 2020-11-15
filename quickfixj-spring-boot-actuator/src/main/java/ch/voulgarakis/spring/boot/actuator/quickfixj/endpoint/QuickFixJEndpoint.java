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

package ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint;

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import quickfix.ConfigError;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.SESSION_NAME;


@Endpoint(id = "quickfixj")
public class QuickFixJEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(QuickFixJEndpoint.class);

    private final SessionSettings sessionSettings;
    private final InternalFixSessions<? extends FixSessionInterface> fixSessions;

    @Autowired
    public QuickFixJEndpoint(SessionSettings sessionSettings,
            InternalFixSessions<? extends FixSessionInterface> fixSessions) {
        this.sessionSettings = sessionSettings;
        this.fixSessions = fixSessions;
    }

    @ReadOperation
    public Map<String, Properties> readProperties() {
        Map<String, Properties> reports = new HashMap<>();
        fixSessions.getFixSessionIDs().forEach((sessionName, sessionID) -> {
            try {
                Properties p = new Properties();
                p.putAll(sessionSettings.getDefaultProperties());
                p.putAll(sessionSettings.getSessionProperties(sessionID));
                p.putIfAbsent(SESSION_NAME, sessionName);
                reports.put(sessionID.toString(), p);
            } catch (ConfigError e) {
                throw new IllegalStateException(e);
            }
        });
        return reports;
    }

    @WriteOperation
    public void logout(@Selector String sessionName, Action action) {
        FixSessionInterface fixSession = fixSessions.retrieveSession(sessionName);
        SessionID sessionId = fixSession.getSessionId();
        Session session = Session.lookupSession(sessionId);
        switch (action) {
            case CONNECT:
                LOG.info("Logging on session: " + sessionName);
                session.logon();
                break;
            case DISCONNECT:
                LOG.info("Logging off session: " + sessionName);
                session.logout("Logout request by QuickFixJ endpoint");
                break;
            default:
                throw new QuickFixJException("Invalid: " + action);
        }
    }

    public enum Action {
        CONNECT,
        DISCONNECT;
    }
}
