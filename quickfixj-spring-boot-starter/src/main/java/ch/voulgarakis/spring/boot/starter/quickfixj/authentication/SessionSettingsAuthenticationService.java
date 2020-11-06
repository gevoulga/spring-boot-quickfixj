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

package ch.voulgarakis.spring.boot.starter.quickfixj.authentication;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import quickfix.*;
import quickfix.field.Password;
import quickfix.field.Username;

import java.util.Objects;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetField;


public class SessionSettingsAuthenticationService implements AuthenticationService {

    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";

    private final SessionSettings sessionSettings;

    public SessionSettingsAuthenticationService(SessionSettings sessionSettings) {
        this.sessionSettings = sessionSettings;
    }

    /**
     * Use the session settings to authenticate the username and password in Login FIX message.
     * <p>
     * in case of initiator: username/password will be set from session settings.
     * in case of acceptor: username/password will be validated against session settings.
     *
     * @param sessionID the sessionID
     * @param message   the fix message
     * @throws RejectLogon if authentication fails (acceptor) or credentials are not present (initiator)
     */
    @Override
    public void authenticate(SessionID sessionID, Message message) throws RejectLogon {
        FixConnectionType fixConnectionType = FixConnectionType.of(sessionSettings);
        authenticate(sessionSettings, sessionID, message, fixConnectionType, USERNAME, new Username());
        authenticate(sessionSettings, sessionID, message, fixConnectionType, PASSWORD, new Password());
    }

    private void authenticate(SessionSettings sessionSettings, SessionID sessionID, Message message,
            FixConnectionType fixConnectionType, String sessionSettingsProperty, StringField field) throws RejectLogon {
        try {
            String value = sessionSettings.getString(sessionID, sessionSettingsProperty);
            //This is a FIX server, compare the credentials from fix message and session settings
            if (fixConnectionType.isAcceptor()) {
                String inMessage = safeGetField(message, field).orElse(null);
                if (!Objects.equals(value, inMessage)) {
                    throw new RejectLogon(
                            String.format("%s [%s] not found", sessionSettingsProperty, inMessage));
                }
            }
            //This is a FIX client, set the credentials in the fix message from the session settings
            else {
                field.setValue(value);
                message.setField(field);
            }
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException("Failed to get " + sessionSettingsProperty + " from properties.",
                    configError);
        }
    }
}
