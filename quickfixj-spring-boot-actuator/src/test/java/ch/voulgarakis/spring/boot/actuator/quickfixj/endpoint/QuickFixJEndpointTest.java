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

package ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint;

import ch.voulgarakis.spring.boot.actuator.quickfixj.QuickFixJAutoConfigurationTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = QuickFixJAutoConfigurationTestConfig.class)
class QuickFixJEndpointTest {

    @Autowired
    private QuickFixJEndpoint quickFixJEndpoint;

    @Test
    void testEndpoint() {
        Map<String, Properties> actual = quickFixJEndpoint.readProperties();

        Properties expected = new Properties();
        expected.put("StartTime", "00:00:00");
        expected.put("ConnectionType", "initiator");
        expected.put("TargetSubID", "TSubID");
        expected.put("EndTime", "00:00:00");
        expected.put("BeginString", "FIX.4.0");
        expected.put("TargetLocationID", "TLocID");
        expected.put("SenderSubID", "SSubID");
        expected.put("SessionName", "FIX.4.0:SCompID/SSubID/SLocID->TCompID/TSubID/TLocID:Qualifier");
        expected.put("ReconnectInterval", "5");
        expected.put("SessionQualifier", "Qualifier");
        expected.put("TargetCompID", "TCompID");
        expected.put("SocketConnectHost", "localhost");
        expected.put("SenderCompID", "SCompID");
        expected.put("HeartBtInt", "30");
        expected.put("SenderLocationID", "SLocID");
        expected.put("SocketConnectPort", "9876");

        assertEquals(1, actual.size());
        Properties props = actual.values().iterator().next();
        assertEquals(props, expected);
    }
}