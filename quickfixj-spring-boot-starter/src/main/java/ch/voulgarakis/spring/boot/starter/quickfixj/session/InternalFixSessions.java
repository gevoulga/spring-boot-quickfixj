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
import org.apache.commons.lang3.tuple.Pair;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;
import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.extractFixSessionName;

/**
 * Contains all the fix session beans
 */
public class InternalFixSessions<T extends AbstractFixSession> {
    protected final Map<SessionID, T> fixSessions;


    public InternalFixSessions(SessionSettings sessionSettings, List<T> sessions,
            Function<SessionID, T> fixSessionCreator) {
        //Ensure unique names for sessions
        if (sessions.size() > 1) {
            List<String> sessionNames = sessions.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            FixSessionUtils.ensureUniqueSessionNames(sessionNames,
                    "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session name.");
        }

        if (sessionSettings.size() == 0) {
            if (sessions.isEmpty()) {
                fixSessions = new HashMap<>();
            } else {
                throw new QuickFixJConfigurationException("No session found in quickfixj session settings.");
            }
        }
        //One session defined in config
        else if (sessionSettings.size() == 1) {
            List<SessionID> sessionIDS = FixSessionUtils.stream(sessionSettings).collect(Collectors.toList());
            SessionID sessionID = sessionIDS.get(0);

            T fixSession;
            //One session bean defined
            if (sessions.size() == 1) {
                fixSession = sessions.get(0);
            }
            //no session bean defined
            else if (sessions.isEmpty()) {
                fixSession = fixSessionCreator.apply(sessionID);
            }
            //Invalid
            else {
                throw new QuickFixJConfigurationException(
                        "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session: " +
                                extractSessionName(sessionSettings, sessionID));
            }

            //Set the sessionId in the fixSession
            fixSession.setSessionId(sessionID);
            //Store the session in the map
            fixSessions = new HashMap<>();
            fixSessions.put(sessionID, fixSession);
        }
        // More than one sessions have been declared
        else {
            //For all the sessions create a map between name and session
            Map<String, T> fixNameSessionMap = sessions.stream()
                    .map(fs -> Pair.of(extractFixSessionName(fs), fs))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

            fixSessions = FixSessionUtils.stream(sessionSettings)
                    .map(sessionID -> {
                        //Get the name of the session
                        String sessionName = extractSessionName(sessionSettings, sessionID);
                        //Try to find the session with the given name in the map
                        T fixSession = Optional.ofNullable(fixNameSessionMap.remove(sessionName))
                                //Or create a new one!
                                .orElseGet(() -> fixSessionCreator.apply(sessionID));
                        //Set the sessionId in the fixSession
                        fixSession.setSessionId(sessionID);
                        return fixSession;
                    })
                    .collect(Collectors.toMap(AbstractFixSession::getSessionId, Function.identity()));
        }
    }

    protected InternalFixSessions(Map<SessionID, T> fixSessions) {
        this.fixSessions = fixSessions;
    }

    protected T retrieveSession(SessionID sessionId) {
        T fixSession = fixSessions.get(sessionId);
        if (Objects.isNull(fixSession)) {
            throw new QuickFixJConfigurationException(
                    String.format("No AbstractFixSession receiver for session [%s] ", sessionId));
        }
        return fixSession;
    }
}
