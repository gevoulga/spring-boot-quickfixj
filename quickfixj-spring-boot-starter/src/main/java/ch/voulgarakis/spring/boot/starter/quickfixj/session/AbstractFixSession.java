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

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;

import java.util.Objects;

public abstract class AbstractFixSession implements FixSessionInterface {

    private SessionID sessionId;

    //--------------------------------------------------
    //-----------------FIX MESSAGE/ERROR----------------
    //--------------------------------------------------

    /**
     * Notifies that a message has been received.
     *
     * @param message the message that has been received.
     */
    protected abstract void received(Message message);

    /**
     * Notifies that a reject message has been received of type:
     * REJECT, BUSINESS_MESSAGE_REJECT, ORDER_CANCEL_REJECT, MARKET_DATA_REQUEST_REJECT, QUOTE_REQUEST_REJECT
     *
     * @param message the actual reject message that has been received.
     */
    protected abstract void error(SessionException message);

    /**
     * Notifies that a fix message has been sent
     *
     * @param message the actual message that has been sent.
     */
    //    protected abstract void sent(Message message);

    //--------------------------------------------------
    //------------------AUTHENTICATION------------------
    //--------------------------------------------------

    /**
     * Handle authentication for this session.
     * If session is an Initiator, this should set the username & password of the session, on the Logon
     * If session is an acceptor, this should authenticate the client based on the Logon Message.
     *
     * @param message in case of initiator: message where username/password should be set.
     *                in case of accepter: the message with the credentials that should be authenticated.
     */
    protected abstract void authenticate(Message message) throws RejectLogon;


    @Override
    public SessionID getSessionId() {
        if (Objects.nonNull(sessionId)) {
            return sessionId;
        } else {
            throw new QuickFixJConfigurationException("SessionId is not set.");
        }
    }

    //--------------------------------------------------
    //--------------------SESSION ID--------------------
    //--------------------------------------------------
    final void setSessionId(SessionID sessionID) {
        if (Objects.isNull(this.sessionId)) {
            this.sessionId = sessionID;
        } else {
            throw new QuickFixJConfigurationException("Not allowed to set SessionId more than once.");
        }
    }

    //--------------------------------------------------
    //------------------SESSION STATUS------------------
    //--------------------------------------------------
    @Override
    public Session getSession() {
        Session session = Session.lookupSession(getSessionId());
        if (Objects.nonNull(session)) {
            return session;
        } else {
            throw new QuickFixJConfigurationException("Session does not exist.");
        }
    }
}
