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

package ch.voulgarakis.spring.boot.starter.quickfixj.session.settings;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.EmptyContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {EmptyContext.class, LiveSessionSettingsTest.LiveSessionSettingsTestContext.class},
        properties = {
                "port=0",
                "sender.compId=TEST_CLIENT",
                "target.compId=FIX"
        })
class LiveSessionSettingsTest {

//    @Autowired
//    private LiveSessionSettings liveSessionSettings;

    @Test
    void refresh() {
//        liveSessionSettings.refresh();
    }

    @TestConfiguration
    @EnableAutoConfiguration
    static class LiveSessionSettingsTestContext {
//        @Bean
//        public SessionSettings sessionSettings(Environment environment, ResourceLoader resourceLoader) {
//            return FixSessionSettings
//                    .loadSettings("classpath:quickfixj.cfg", environment, resourceLoader);
//        }

//        @Bean("TEST_SESSION")
//        public FixSession fixSession() {
//            return new DefaultFixSession() {
//                @Override
//                protected void received(Message message) {
//                }
//
//                @Override
//                protected void error(SessionException message) {
//                }
//
//                @Override
//                protected void loggedOn() {
//                }
//            };
//        }
    }
}