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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

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

public class ReactiveFixSessions implements InternalFixSessions<ReactiveFixSession> {

    private final Map<SessionID, ReactiveFixSession> fixSessions;
    private final Map<String, SessionID> nameToSessionId;

    public ReactiveFixSessions(GenericApplicationContext applicationContext, SessionSettings sessionSettings) {
        BiFunction<String, SessionID, BeanDefinition> fixSessionBeanDefinitionCreator =
                (sessionName, sessionID) -> BeanDefinitionBuilder
                        .genericBeanDefinition(NamedReactiveFixSessionImpl.class)
                        .addConstructorArgValue(sessionName)
                        .addConstructorArgValue(sessionID)
                        .getBeanDefinition();

        DynamicFixSessionBeanRegistration<ReactiveFixSession> fixSessionDynamicFixSessionBeanRegistration =
                new DynamicFixSessionBeanRegistration<>(applicationContext, sessionSettings,
                        fixSessionBeanDefinitionCreator, ReactiveFixSession.class);

        Pair<Map<SessionID, ReactiveFixSession>, Map<String, SessionID>> sessionBeans =
                fixSessionDynamicFixSessionBeanRegistration.registerSessionBeans();

        fixSessions = sessionBeans.getLeft();
        nameToSessionId = sessionBeans.getRight();
    }

    @Override
    public Map<SessionID, ReactiveFixSession> getFixSessions() {
        return fixSessions;
    }

    @Override
    public Map<String, SessionID> getFixSessionIDs() {
        return nameToSessionId;
    }
}
