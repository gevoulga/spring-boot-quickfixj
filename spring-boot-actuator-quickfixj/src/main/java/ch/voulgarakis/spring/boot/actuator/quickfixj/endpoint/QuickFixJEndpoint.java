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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static quickfix.SessionID.NOT_SET;


@Endpoint(id = "quickfixj")
public class QuickFixJEndpoint {

    private final SessionSettings sessionSettings;

    @Autowired
    public QuickFixJEndpoint(SessionSettings sessionSettings) {
        this.sessionSettings = sessionSettings;
    }

    @ReadOperation
    public Map<String, Properties> readProperties() {
        Map<String, Properties> reports = new HashMap<>();
        sessionSettings.sectionIterator().forEachRemaining(sessionId -> {
            try {
                Properties p = new Properties();
                p.putAll(sessionSettings.getDefaultProperties());
                p.putAll(sessionSettings.getSessionProperties(sessionId));
                p.putAll(addSessionIdProperties(sessionId));

                reports.put(sessionId.toString(), p);
            } catch (ConfigError e) {
                throw new IllegalStateException(e);
            }
        });
        return reports;
    }

    private Properties addSessionIdProperties(SessionID sessionID) {
        Properties properties = new Properties();
        properties.put("BeginString", sessionID.getBeginString());
        properties.put("SenderCompID", sessionID.getSenderCompID());
        String senderSubID = sessionID.getSenderSubID();
        if (!senderSubID.equals(NOT_SET)) {
            properties.put("SenderSubID", senderSubID);
        }
        String senderLocationID = sessionID.getSenderLocationID();
        if (!senderLocationID.equals(NOT_SET)) {
            properties.put("SenderLocationID", senderLocationID);
        }
        properties.put("TargetCompID", sessionID.getTargetCompID());
        String targetSubID = sessionID.getTargetSubID();
        if (!targetSubID.equals(NOT_SET)) {
            properties.put("TargetSubID", targetSubID);
        }
        String targetLocationID = sessionID.getTargetLocationID();
        if (!targetLocationID.equals(NOT_SET)) {
            properties.put("TargetLocationID", targetLocationID);
        }
        String sessionQualifier = sessionID.getSessionQualifier();
        if (!sessionQualifier.equals(NOT_SET)) {
            properties.put("Qualifier", sessionQualifier);
        }

        return properties;
    }
}
