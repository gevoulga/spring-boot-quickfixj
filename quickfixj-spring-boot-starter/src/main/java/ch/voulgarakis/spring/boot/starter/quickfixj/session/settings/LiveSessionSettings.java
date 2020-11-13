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

import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils;
import quickfix.*;
import quickfix.mina.acceptor.AbstractSocketAcceptor;
import quickfix.mina.initiator.AbstractSocketInitiator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LiveSessionSettings {

    private final Connector connector;
    private final FixSessionSettings fixSessionSettings;
    private final SessionSettings sessionSettings;
    private final OnSettingsChangeDynamicAcceptorSessionProvider acceptorSessionProvider;

    public LiveSessionSettings(Connector connector,
            FixSessionSettings fixSessionSettings,
            SessionSettings sessionSettings,
            OnSettingsChangeDynamicAcceptorSessionProvider acceptorSessionProvider) {
        this.connector = connector;
        this.fixSessionSettings = fixSessionSettings;
        this.sessionSettings = sessionSettings;
        this.acceptorSessionProvider = acceptorSessionProvider;
    }

    public void refresh() throws ConfigError, IOException {
        //Create the new session settings
        SessionSettings newSessionSettings = null;
        //Make sure the new session names are unique
        FixSessionUtils.ensureUniqueSessionNames(newSessionSettings);

        //Existing sessions
        Set<SessionID> existingSessions = FixSessionUtils.stream(sessionSettings).collect(Collectors.toSet());

        //Iterate over the new sessions sessions
        Iterator<SessionID> iterator = newSessionSettings.sectionIterator();
        while (iterator.hasNext()) {
            SessionID sessionID = iterator.next();
            Map<Object, Object> newSettings = newSessionSettings.get(sessionID).toMap();

            //Does the session already exist?
            Map<Object, Object> existingSettings = existingSessions.contains(sessionID) ?
                    sessionSettings.get(sessionID).toMap() : null;

            //Are they any different from the new session settings?
            if (!Objects.equals(existingSettings, newSettings)) {
                //If previous session already exists, we need to close the session
                if (Objects.nonNull(existingSettings)) {
                    removeSession(sessionID);
                }

                //Create the new session
                if (connector instanceof AbstractSocketAcceptor) {
                    acceptorSessionProvider.getSession(sessionID, (AbstractSocketAcceptor) connector);
                } else if (connector instanceof AbstractSocketInitiator) {
                    ((AbstractSocketInitiator) connector).createDynamicSession(sessionID);
                }
            }
        }

    }

    private void removeSession(SessionID sessionID) throws IOException {
        //Lookup  the session
        Session session = Session.lookupSession(sessionID);
        //Logout and close
        session.logout();
        session.close();
        //Remove the session from the connector
        if (connector instanceof AbstractSocketAcceptor) {
            ((AbstractSocketAcceptor) connector).removeDynamicSession(sessionID);
        } else if (connector instanceof AbstractSocketInitiator) {
            ((AbstractSocketInitiator) connector).removeDynamicSession(sessionID);
        }
    }
}
