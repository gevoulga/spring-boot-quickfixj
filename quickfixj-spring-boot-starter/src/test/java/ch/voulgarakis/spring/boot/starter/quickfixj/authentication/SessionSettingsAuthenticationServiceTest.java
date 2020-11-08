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

import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import ch.voulgarakis.spring.boot.starter.quickfixj.utils.FixMessageBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quickfix.ConfigError;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.field.Password;
import quickfix.field.Username;
import quickfix.fix43.Logon;

import static ch.voulgarakis.spring.boot.starter.quickfixj.utils.FixMessageComparator.FIX_MESSAGE_COMPARATOR;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionSettingsAuthenticationServiceTest {

    private static SessionSettings sessionSettings;

    @BeforeAll
    static void init() throws ConfigError {
        sessionSettings = new SessionSettings("quickfixj-with-credentials.cfg");
    }

    @Test
    void authenticateInAcceptorOK() throws RejectLogon {
        SessionSettingsAuthenticationService authenticationService =
                new SessionSettingsAuthenticationService(sessionSettings, FixConnectionType.ACCEPTOR);

        SessionID sessionID = new SessionID("FIX.4.3:TEST_CLIENT1->FIX");
        Logon logon = new Logon();
        logon.set(new Username("user"));
        logon.set(new Password("pass"));

        //Should succeed
        authenticationService.authenticate(sessionID, logon);
    }

    @Test
    void authenticateInAcceptorFails() {
        SessionSettingsAuthenticationService authenticationService =
                new SessionSettingsAuthenticationService(sessionSettings, FixConnectionType.ACCEPTOR);

        SessionID sessionID = new SessionID("FIX.4.3:TEST_CLIENT1->FIX");
        Logon logon = new Logon();
        logon.set(new Username("user2"));
        logon.set(new Password("pass2"));

        //Should succeed
        assertThrows(RejectLogon.class, () -> authenticationService.authenticate(sessionID, logon));
    }

    @Test
    void authenticateInInitiatorSetsCredentials() throws RejectLogon {
        SessionSettingsAuthenticationService authenticationService =
                new SessionSettingsAuthenticationService(sessionSettings, FixConnectionType.INITIATOR);

        SessionID sessionID = new SessionID("FIX.4.3:TEST_CLIENT2->FIX");
        Logon logon = new Logon();

        //Should set the credentials in the fix message
        authenticationService.authenticate(sessionID, logon);

        Logon expected = new FixMessageBuilder<>(new Logon())
                .add(new Username("user2"))
                .add(new Password("pass2"))
                .build();

        FIX_MESSAGE_COMPARATOR.assertFixMessagesEquals(expected,logon);

    }
}