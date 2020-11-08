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
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixSessionsTest {

    private static SessionSettings sessionSettings;

    @BeforeAll
    static void loadSettings() throws IOException, ConfigError {
        sessionSettings =
                new FixSessionSettings(null, null, null)
                        .createSessionSettings();
    }

    @Test
    void testCreateSession() {
        DefaultFixSession defaultFixSession = new DefaultFixSession() {
            @Override
            protected void received(Message message) {
            }

            @Override
            protected void error(SessionException message) {
            }

            @Override
            protected void loggedOn() {
            }
        };

        assertThrows(QuickFixJConfigurationException.class, defaultFixSession::getSessionId);

        //Add the Fix Session bean into the registry (fix Sessions)
        FixSessions fixSessions = new FixSessions(sessionSettings, Collections.singletonList(defaultFixSession));

        //Now we should have allocated a SessionID
        assertEquals(new SessionID("FIX.4.3", "TEST_CLIENT", "FIX"), defaultFixSession.getSessionId());
        assertEquals(1, fixSessions.fixSessions.size());
    }

}