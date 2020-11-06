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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.List;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;

public class FixSessions extends InternalFixSessions<AbstractFixSession> {

    public FixSessions(SessionSettings sessionSettings, List<AbstractFixSession> sessions) {
        super(sessionSettings, sessions, sessionID -> {
            String sessionName = extractSessionName(sessionSettings, sessionID);
            List<String> sessionNames = sessions.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            throw new QuickFixJConfigurationException(
                    "Could not find a session with name [" + sessionName + "] in sessions: " + sessionNames);
        });
    }

    public FixSession get(SessionID sessionID) {
        AbstractFixSession abstractFixSession = retrieveSession(sessionID);
        if (abstractFixSession instanceof FixSession) {
            return (FixSession) abstractFixSession;
        } else {
            throw new QuickFixJException("Could not convert to FixSession: " + abstractFixSession);
        }
    }
}
