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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import quickfix.*;
import quickfix.mina.SessionConnector;
import quickfix.mina.acceptor.AcceptorSessionProvider;

public class OnSettingsChangeDynamicAcceptorSessionProvider implements AcceptorSessionProvider {

    private final SessionSettings sessionSettings;
    private final DefaultSessionFactory sessionFactory;
    private final SessionConnector sessionConnector;

    public OnSettingsChangeDynamicAcceptorSessionProvider(SessionSettings sessionSettings,
            SessionConnector sessionConnector, Application application, MessageStoreFactory messageStoreFactory,
            LogFactory logFactory, MessageFactory messageFactory) {
        this.sessionSettings = sessionSettings;
        this.sessionConnector = sessionConnector;
        this.sessionFactory = new DefaultSessionFactory(application, messageStoreFactory, logFactory, messageFactory);
    }

    @Override
    public Session getSession(SessionID sessionID, SessionConnector connector) {
        try {
            Session session = sessionFactory.create(sessionID, sessionSettings);
            if (sessionConnector != null) {
                sessionConnector.addDynamicSession(session);
            }
            return session;
        } catch (ConfigError configError) {
            throw new QuickFixJException("Failed to create a dynamic session for sessionID: " + sessionID, configError);
        }
    }
}
