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

package ch.voulgarakis.spring.boot.starter.quickfixj.fix.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FixSessionsTest {

    private static SessionSettings sessionSettings;
    private static GenericApplicationContext applicationContext;
    private static final SessionID sessionID = new SessionID("FIX.4.3", "TEST_CLIENT", "FIX");

    @BeforeAll
    static void loadSettings() throws IOException, ConfigError {
        applicationContext = new GenericXmlApplicationContext();
        applicationContext.refresh();
        sessionSettings =
                new FixSessionSettings(null, null, null)
                        .createSessionSettings();
    }

    @Test
    void testCreateSession() {
        //The bean in not yet registered
        assertThrows(BeansException.class, () -> applicationContext.getBean(FixSession.class));

        //Create the fix sessions. This should register the session bean in the context dynamically
        FixSessions fixSessions = new FixSessions(applicationContext, sessionSettings);
        FixSession fixSession = applicationContext.getBean(FixSession.class);

        //Now we should have allocated a SessionID
        assertEquals(sessionID, fixSession.getSessionId());
        assertEquals(1, fixSessions.getFixSessions().size());
    }

}