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

import ch.voulgarakis.spring.boot.starter.quickfixj.EmptyContext;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.settings.SessionSettingsEnhancer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.IOException;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EmptyContext.class,
        properties = {
                "port=0",
                "sender.compId=TEST_CLIENT",
                "target.compId=FIX"
        })
public class FixSessionSettingsSpringTest {

    @Autowired
    private Environment environment;
    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    void createSessionSettings() throws ConfigError, IOException {
        Resource quickfixjConfig = FixSessionSettings.findQuickfixjConfig("quickfixj-with-placeholders.cfg");
        SessionSettings sessionSettings = new SessionSettings(quickfixjConfig.getInputStream());
        SessionSettingsEnhancer enhancer = new SessionSettingsEnhancer(environment, resourceLoader);
        SessionSettings actual = enhancer.enhanceSettingSettings(sessionSettings);

        SessionSettings expected = new SessionSettings("quickfixj.cfg");

        //We can't do simply assertEquals(expected,actual) because of ordering of properties in session is not certain to be the same
        assertSessionSettingsEquals(sessionSettings, actual, expected);
    }

    private void assertSessionSettingsEquals(SessionSettings sessionSettings, SessionSettings actual,
            SessionSettings expected) throws ConfigError {
        assertEquals(expected.size(),actual.size());

        //For all the sessions
        Iterator<SessionID> it = sessionSettings.sectionIterator();
        while (it.hasNext()) {
            SessionID sessionID = it.next();
            assertEquals(sessionSettings.get(sessionID).toMap(), actual.get(sessionID).toMap());
        }
        //and for default sessions
        assertEquals(expected.getDefaultProperties(), actual.getDefaultProperties());
    }
}