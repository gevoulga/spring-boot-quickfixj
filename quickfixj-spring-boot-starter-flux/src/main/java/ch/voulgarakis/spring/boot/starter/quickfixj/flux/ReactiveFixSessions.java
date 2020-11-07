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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import org.apache.commons.lang3.StringUtils;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.List;
import java.util.Objects;

public class ReactiveFixSessions extends InternalFixSessions<ReactiveAbstractFixSession> {

    public ReactiveFixSessions(SessionSettings sessionSettings, List<ReactiveAbstractFixSession> sessions) {
        super(sessionSettings, sessions, ReactiveAbstractFixSession::new);
    }

    public ReactiveFixSession get(SessionID sessionID) {
        return retrieveSession(sessionID);
    }

    public ReactiveFixSession get() {
        //Only used when one session in registry
        SessionID sessionID = nameToSessionId.values().iterator().next();
        return get(sessionID);
    }

    public ReactiveFixSession get(String sessionName) {
        SessionID sessionID;
        //If only one session in registry, then we allow empty/null strings
        if (StringUtils.isBlank(sessionName) && nameToSessionId.size() == 1) {
            sessionID = nameToSessionId.values().iterator().next();
        } else {
            sessionID = nameToSessionId.get(sessionName);
        }

        if (Objects.isNull(sessionID)) {
            try {
                sessionID = new SessionID(sessionName);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("No session found for sessionName: " + sessionName);
            }
        }
        return get(sessionID);
    }
}
