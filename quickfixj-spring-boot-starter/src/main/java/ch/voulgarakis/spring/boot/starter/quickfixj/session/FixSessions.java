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
import org.apache.commons.lang3.StringUtils;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.List;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.extractFixSessionName;

public class FixSessions extends InternalFixSessions<AbstractFixSession> {

    public FixSessions(SessionSettings sessionSettings, List<AbstractFixSession> sessions) {
        super(sessionSettings, (sessionName, sessionID) -> {
            if (sessions.size() == 1) {
                return sessions.get(0);
            }

            List<AbstractFixSession> sessionsMatching = sessions.stream()
                    .filter(session -> {
                        String ssessionName = extractFixSessionName(session);
                        return StringUtils.equalsIgnoreCase(ssessionName, sessionName);
                    })
                    .collect(Collectors.toList());

            if (sessionsMatching.isEmpty()) {
                throw new QuickFixJConfigurationException(
                        "Could not find a session with name [" + sessionName + "]");
            } else if (sessionsMatching.size() > 1) {
                throw new QuickFixJConfigurationException(
                        "More than one sessions found for [" + sessionName + "]");
            }
            return sessionsMatching.get(0);
        });

        //Ensure unique names for sessions
        if (sessions.size() > 1) {
            List<String> sessionNames = sessions.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            FixSessionUtils.ensureUniqueSessionNames(sessionNames,
                    "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session name.");
        }

        //Make sure there's a session mapping to session settings
        if (sessionSettings.size() != 0 && sessions.isEmpty()) {
            throw new QuickFixJConfigurationException("No session found in quickfixj session settings.");
        }
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
