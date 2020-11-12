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

package ch.voulgarakis.spring.boot.starter.quickfixj.fix.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.DynamicFixSessionBeanRegistration;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.Map;
import java.util.function.BiFunction;

public class FixSessions implements InternalFixSessions<FixSession> {

    private final Map<SessionID, FixSession> fixSessions;
    private final Map<String, SessionID> nameToSessionId;

    public FixSessions(GenericApplicationContext applicationContext, SessionSettings sessionSettings) {
        BiFunction<String, SessionID, BeanDefinition> fixSessionBeanDefinitionCreator =
                (sessionName, sessionID) -> BeanDefinitionBuilder
                        .genericBeanDefinition(NamedFixSessionImpl.class)
                        .addConstructorArgValue(sessionName)
                        .addConstructorArgValue(sessionID)
                        .getBeanDefinition();

        DynamicFixSessionBeanRegistration<FixSession> fixSessionDynamicFixSessionBeanRegistration =
                new DynamicFixSessionBeanRegistration<>(applicationContext, sessionSettings,
                        fixSessionBeanDefinitionCreator, FixSession.class);

        Pair<Map<SessionID, FixSession>, Map<String, SessionID>> sessionBeans =
                fixSessionDynamicFixSessionBeanRegistration.registerSessionBeans();

        fixSessions = sessionBeans.getLeft();
        nameToSessionId = sessionBeans.getRight();
    }

    @Override
    public Map<SessionID, FixSession> getFixSessions() {
        return fixSessions;
    }

    @Override
    public Map<String, SessionID> getFixSessionIDs() {
        return nameToSessionId;
    }
}
