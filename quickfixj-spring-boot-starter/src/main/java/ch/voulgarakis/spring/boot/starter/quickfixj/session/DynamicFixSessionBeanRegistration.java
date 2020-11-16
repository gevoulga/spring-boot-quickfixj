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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;

public class DynamicFixSessionBeanRegistration<T extends FixSessionInterface> {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicFixSessionBeanRegistration.class);

    private final GenericApplicationContext applicationContext;
    private final SessionSettings sessionSettings;
    private final BiFunction<String, SessionID, BeanDefinition> fixSessionBeanDefinitionCreator;
    private final Class<? extends T> sessionBeanType;

    public DynamicFixSessionBeanRegistration(
            GenericApplicationContext applicationContext, SessionSettings sessionSettings,
            BiFunction<String, SessionID, BeanDefinition> fixSessionBeanDefinitionCreator,
            Class<? extends T> sessionBeanType) {
        this.applicationContext = applicationContext;
        this.sessionSettings = sessionSettings;
        this.fixSessionBeanDefinitionCreator = fixSessionBeanDefinitionCreator;
        this.sessionBeanType = sessionBeanType;
    }

    public Pair<Map<SessionID, T>, Map<String, SessionID>> registerSessionBeans() {
        Pair<Map<SessionID, T>, Map<String, SessionID>> response;

        //No sessions defined
        if (sessionSettings.size() == 0) {
            response = Pair.of(Collections.emptyMap(), Collections.emptyMap());
        }
        //One session defined in config
        else if (sessionSettings.size() == 1) {
            List<SessionID> sessionIDS = FixSessionUtils.stream(sessionSettings).collect(Collectors.toList());
            SessionID sessionID = sessionIDS.get(0);
            String sessionName = extractSessionName(sessionSettings, sessionID);

            //Get any session beans already defined
            T fixSession = getDefinedSessionBean(sessionID, sessionName);
            //Store the session in the map
            response = Pair.of(Collections.singletonMap(sessionID, fixSession),
                    Collections.singletonMap(sessionName, sessionID));
        }
        // More than one sessions have been declared
        else {
            //Extract the session bean and the name and add them in the registry
            Map<SessionID, T> sessionBeans = new HashMap<>();
            Map<String, SessionID> sessionNamesToSessionID = new HashMap<>();
            //For all the sessions defined in the session settings file
            FixSessionUtils.stream(sessionSettings)
                    .forEach(sessionID -> {
                        //Get the name of the session
                        String sessionName = extractSessionName(sessionSettings, sessionID);
                        //Try to find the session with the given name in the map
                        T sessionBean = createSessionBean(sessionName, sessionID);

                        //Save to the maps
                        sessionNamesToSessionID.put(sessionName, sessionID);
                        sessionBeans.put(sessionID, sessionBean);
                    });
            response = Pair.of(sessionBeans, sessionNamesToSessionID);
        }

        //Assert that all the session beans have been allocated a corresponding SessionID
        applicationContext.getBeansOfType(FixSessionInterface.class)
                .forEach((key, session) -> {
                    try {
                        session.getSessionId();
                    } catch (Exception e) {
                        throw new QuickFixJConfigurationException(
                                "No SessionID allocated to session bean: " + key, e);
                    }
                });

        return response;
    }

    private T getDefinedSessionBean(SessionID sessionID, String sessionName) {
        Map<String, ? extends T> sessionBeans = applicationContext.getBeansOfType(sessionBeanType);
        T fixSession;
        if (sessionBeans.isEmpty()) {
            fixSession = createSessionBean(sessionName, sessionID);
        } else if (sessionBeans.size() == 1) {
            //Set the (ONLY!) session bean defined
            fixSession = sessionBeans.values().iterator().next();
            //Set the sessionId in the fixSession
            if (fixSession instanceof AbstractFixSession) {
                ((AbstractFixSession) fixSession).setSessionId(sessionID);
                ((AbstractFixSession) fixSession).setSessionName(sessionName);
            }
        }
        //Too many session beans defined for the given session settings?
        else {
            throw new QuickFixJConfigurationException(String.format(
                    "Too many session beans defined [beanNames=%s] for session settings with only one session specified [sessionName=%s]."
                    , sessionBeans.keySet(), sessionName));
        }
        return fixSession;
    }

    private T createSessionBean(String sessionName, SessionID sessionID) {
        T fixSession;

        //Check if the session bean is already defined
        //If not create it
        if (!applicationContext.containsBean(sessionName)) {

            //We do not use "new AbstractFixSession(sessionID)" directly.
            // Instead we create the reactive fix session bean definition.
            BeanDefinition beanDefinition = fixSessionBeanDefinitionCreator.apply(sessionName, sessionID);

            //Register the bean definition in spring
            applicationContext.registerBeanDefinition(sessionName, beanDefinition);

            //Retrieve the actual bean (will also force its instantiation)
            fixSession = applicationContext.getBean(sessionName, sessionBeanType);
            LOG.info("FixSession created with bean name='{}', and SessionID={}", sessionName, sessionID);
        }
        //Otherwise simply get it from the context
        else {
            fixSession = applicationContext.getBean(sessionName, sessionBeanType);
            //Set the sessionId in the fixSession
            if (fixSession instanceof AbstractFixSession) {
                ((AbstractFixSession) fixSession).setSessionId(sessionID);
                ((AbstractFixSession) fixSession).setSessionName(sessionName);
            }
        }

        return fixSession;
    }
}
