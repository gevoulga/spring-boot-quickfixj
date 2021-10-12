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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.SYSTEM_VARIABLE_QUICKFIXJ_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FixSessionSettingsTest {

    @Test
    public void testExtractSessionName() throws ConfigError {
        SessionSettings sessionSettings = new SessionSettings("quickfixj.cfg");
        String sessionName = FixSessionSettings
                .extractSessionName(sessionSettings, new SessionID("FIX.4.3", "TEST_CLIENT", "FIX"));

        assertEquals("TEST_SESSION", sessionName);
    }

    @Test
    void findQuickfixjConfig() {
        //Should be OK
        FixSessionSettings.findQuickfixjConfig("classpath:quickfixj.cfg");

        //Should still be OK
        System.setProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG, "classpath:quickfixj-with-placeholders.cfg");
        FixSessionSettings.findQuickfixjConfig(null);

        //Should fail (invalid specified file)
        assertThrows(QuickFixJSettingsNotFoundException.class, () ->
                FixSessionSettings.findQuickfixjConfig("classpath:quickfixj-does-not-exist.cfg"));

        //Nothing specified, should be OK
        System.clearProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG);
        FixSessionSettings.findQuickfixjConfig(null);
    }
}