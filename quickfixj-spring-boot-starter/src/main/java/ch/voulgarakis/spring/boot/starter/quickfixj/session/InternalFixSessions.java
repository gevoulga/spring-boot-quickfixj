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

import java.util.Map;
import java.util.Objects;

/**
 * Contains all the fix session beans
 */
public interface InternalFixSessions<T> {

    Map<SessionID, T> getFixSessions();

    Map<String, SessionID> getFixSessionIDs();

    default T retrieveSession(String sessionName) {
        SessionID sessionId = getFixSessionIDs().get(sessionName);
        if (Objects.isNull(sessionId)) {
            throw new QuickFixJConfigurationException(
                    String.format("No AbstractFixSession receiver for session name [%s] ", sessionName));
        }
        return retrieveSession(sessionId);
    }

    default T retrieveSession(SessionID sessionId) {
        T fixSession = getFixSessions().get(sessionId);
        if (Objects.isNull(fixSession)) {
            throw new QuickFixJConfigurationException(
                    String.format("No AbstractFixSession receiver for session [%s] ", sessionId));
        }
        return fixSession;
    }
}
