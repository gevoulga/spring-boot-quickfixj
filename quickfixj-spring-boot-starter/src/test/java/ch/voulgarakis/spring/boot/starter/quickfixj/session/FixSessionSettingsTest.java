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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringRunner;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.SessionSettings;

import static junit.framework.TestCase.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmptyContext.class, FixSessionSettingsTest.FixSessionSettingsTestContext.class},
        properties = {
                "port=0",
                "sender.compId=TEST_CLIENT",
                "target.compId=FIX"
        })
public class FixSessionSettingsTest {

    @Autowired
    private SessionSettings sessionSettings;
    @Autowired
    private Environment environment;
    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void testLoadSettings() {
        SessionSettings actual = FixSessionSettings
                .loadSettings("classpath:quickfixj-with-placeholders.cfg", environment, resourceLoader);

        assertEquals(sessionSettings.toString(), actual.toString());
    }

    @Test
    public void testCreateConnector() {
    }

    @Test
    public void testExtractSessionName() {
        String sessionName = FixSessionSettings
                .extractSessionName(sessionSettings, new SessionID("FIX.4.3", "TEST_CLIENT", "FIX"));

        assertEquals("TEST_SESSION", sessionName);
    }

    @Test
    public void testAuthenticate() {
    }

    @Test
    public void testCreateSession() {
        AbstractFixSession abstractFixSession = new AbstractFixSession(sessionSettings) {

            @Override
            protected void received(Message message) {
                //nth to do
            }

            @Override
            protected void error(SessionException message) {
//nth to do
            }

            @Override
            protected void authenticate(Message message) {
//nth to do
            }
        };

        assertEquals(abstractFixSession.getSessionId(), new SessionID("FIX.4.3", "TEST_CLIENT", "FIX"));
    }

    @TestConfiguration
    @EnableAutoConfiguration
    static class FixSessionSettingsTestContext {
        @Bean
        public SessionSettings sessionSettings(Environment environment, ResourceLoader resourceLoader) {
            return FixSessionSettings
                    .loadSettings("classpath:quickfixj.cfg", environment, resourceLoader);
        }
    }
}