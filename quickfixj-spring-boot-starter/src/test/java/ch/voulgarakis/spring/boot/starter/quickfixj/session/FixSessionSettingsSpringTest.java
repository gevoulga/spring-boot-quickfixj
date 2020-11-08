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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.ConfigError;
import quickfix.SessionSettings;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

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
        FixSessionSettings fixSessionSettings =
                new FixSessionSettings(environment, resourceLoader, "quickfixj-with-placeholders.cfg");
        SessionSettings actual = fixSessionSettings.createSessionSettings();

        SessionSettings expected = new SessionSettings("quickfixj.cfg");
        assertEquals(expected.toString(), actual.toString());
    }
}