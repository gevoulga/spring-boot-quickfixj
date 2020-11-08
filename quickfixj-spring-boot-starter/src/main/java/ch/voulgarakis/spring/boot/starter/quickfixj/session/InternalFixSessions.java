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
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;

/**
 * Contains all the fix session beans
 */
public class InternalFixSessions<T extends AbstractFixSession> {
    protected final Map<SessionID, T> fixSessions;
    protected final Map<String, SessionID> nameToSessionId;


    public InternalFixSessions(SessionSettings sessionSettings, BiFunction<String, SessionID, T> fixSessionProvider) {


        if (sessionSettings.size() == 0) {
            fixSessions = Collections.emptyMap();
            nameToSessionId = Collections.emptyMap();
        }
        //One session defined in config
        else if (sessionSettings.size() == 1) {
            List<SessionID> sessionIDS = FixSessionUtils.stream(sessionSettings).collect(Collectors.toList());
            SessionID sessionID = sessionIDS.get(0);
            String sessionName = extractSessionName(sessionSettings, sessionID);
            T fixSession = fixSessionProvider.apply(sessionName, sessionID);

            //Set the sessionId in the fixSession
            fixSession.setSessionId(sessionID);
            //Store the session in the map
            fixSessions = Collections.singletonMap(sessionID, fixSession);
            nameToSessionId = Collections.singletonMap(sessionName, sessionID);
        }
        // More than one sessions have been declared
        else {
            nameToSessionId = new HashMap<>();
            fixSessions = FixSessionUtils.stream(sessionSettings)
                    .map(sessionID -> {
                        //Get the name of the session
                        String sessionName = extractSessionName(sessionSettings, sessionID);
                        nameToSessionId.put(sessionName, sessionID);
                        //Try to find the session with the given name in the map
                        T fixSession = fixSessionProvider.apply(sessionName, sessionID);
                        //Set the sessionId in the fixSession
                        fixSession.setSessionId(sessionID);
                        return fixSession;
                    })
                    .collect(Collectors.toMap(AbstractFixSession::getSessionId, Function.identity()));
        }
    }

    public Map<SessionID, T> getFixSessions() {
        return fixSessions;
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
