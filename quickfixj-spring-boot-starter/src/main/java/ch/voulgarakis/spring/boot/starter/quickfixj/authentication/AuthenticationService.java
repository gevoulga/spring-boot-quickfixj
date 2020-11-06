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

import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;

/**
 * A service that will authenticate LOGIN FIX messages.
 * If session is an Initiator, this should set the username & password of the session, on the Logon
 * If session is an acceptor, this should authenticate the client based on the Logon Message.
 * <p>
 * in case of initiator: message where username/password should be set.
 * in case of acceptor: the message with the credentials that should be authenticated.
 */
public interface AuthenticationService {

    /**
     * Authenticate the username and password in Login FIX message.
     * <p>
     * in case of initiator: username/password will be set from session settings.
     * in case of acceptor: username/password will be validated against session settings.
     *
     * @param sessionID the sessionID
     * @param message   the fix message
     * @throws RejectLogon if authentication fails (acceptor) or credentials are not present (initiator)
     */
    void authenticate(SessionID sessionID, Message message) throws RejectLogon;
}
