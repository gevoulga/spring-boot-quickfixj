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

package ch.voulgarakis.spring.boot.actuator.quickfixj.health;

import ch.voulgarakis.spring.boot.actuator.quickfixj.QuickFixJAutoConfigurationTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.SessionID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = QuickFixJAutoConfigurationTestConfig.class)
class QuickFixJHealthIndicatorTest {

    @Autowired
    private QuickFixJHealthIndicator quickFixJHealthIndicator;

    //TODO increase test coverage
    @Test
    void testHealth() {
        Health health = quickFixJHealthIndicator.health();

        SessionID sessionID = new SessionID("FIX.4.0:SCompID/SSubID/SLocID->TCompID/TSubID/TLocID:Qualifier");
        Health expected = Health.down()
                .withDetail(sessionID.toString(),"FIX connection never established")
                .build();
        assertEquals(expected, health);
    }
}