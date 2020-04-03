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

import quickfix.*;

public enum FixConnectionType {
    ACCEPTOR(false, true),
    ACCEPTOR_THREADED(true, true),
    INITIATOR(false, false),
    INITIATOR_THREADED(true, false);

    private static final String CONCURRENT = "Concurrent";

    private final boolean isConcurrent;
    private final boolean isAcceptor;

    FixConnectionType(boolean isConcurrent, boolean isAcceptor) {
        this.isConcurrent = isConcurrent;
        this.isAcceptor = isAcceptor;
    }

    public static FixConnectionType of(SessionSettings sessionSettings) throws ConfigError, FieldConvertError {
        String connectionType = sessionSettings.getString(SessionFactory.SETTING_CONNECTION_TYPE);
        boolean isThreaded = sessionSettings.isSetting(CONCURRENT) && sessionSettings.getBool(CONCURRENT);

        if (connectionType.equals(SessionFactory.ACCEPTOR_CONNECTION_TYPE)) {
            return isThreaded ? ACCEPTOR_THREADED : ACCEPTOR;
        } else if (connectionType.equals(SessionFactory.INITIATOR_CONNECTION_TYPE)) {
            return isThreaded ? INITIATOR_THREADED : INITIATOR;
        } else {
            throw new ConfigError("Failed to determine " + SessionFactory.SETTING_CONNECTION_TYPE);
        }
    }

    public boolean isAcceptor() {
        return isAcceptor;
    }

    public Connector createConnector(Application application, MessageStoreFactory messageStoreFactory,
                                     SessionSettings sessionSettings, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError {
        if (isAcceptor) {
            if (isConcurrent) {
                return new ThreadedSocketAcceptor(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            } else {
                return new SocketAcceptor(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            }
        } else {
            if (isConcurrent) {
                return new ThreadedSocketInitiator(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            } else {
                return new SocketInitiator(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            }
        }
    }


}