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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import quickfix.ConfigError;
import quickfix.Dictionary;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.stream;

public class SessionSettingsEnhancer {

    private static final String DATA_DICTIONARY = "DataDictionary";

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    public SessionSettingsEnhancer(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    public SessionSettings enhanceSettingSettings(SessionSettings sessionSettings) throws ConfigError {

        //Replace placeholders on session settings level
        sessionSettings.setVariableValues(new Properties() {
            @Override
            public String getProperty(String key) {
                return environment.getProperty(key);
            }
        });

        //Replace placeholders
        Iterator<SessionID> it = sessionSettings.sectionIterator();
        while (it.hasNext()) {
            SessionID sessionID = it.next();
            enhanceSessionIdSettings(sessionSettings, sessionID);
        }
        //Enhance the Default settings
        enhanceSessionIdSettings(sessionSettings, new SessionID("DEFAULT", "", ""));

        //Replace the directories specified to a format understood by quickfixj
        resolveDirectories(sessionSettings);

        //Make sure the session names are unique
        FixSessionUtils.ensureUniqueSessionNames(sessionSettings);

        return sessionSettings;
    }

    private void enhanceSessionIdSettings(SessionSettings sessionSettings, SessionID sessionID) throws ConfigError {
        //Get the existing settings
        Dictionary dictionary = sessionSettings.get(sessionID);
        Map<Object, Object> map = dictionary.toMap();

        //Replace any placeholders using spring environment
        map.forEach((key, oldProperty) -> {
            if (oldProperty instanceof String) {
                String replacedProperties = environment.resolvePlaceholders((String) oldProperty);
                map.put(key, replacedProperties);
            }
        });
        //Put back the replaced properties!
        sessionSettings.set(sessionID, dictionary);
    }

    private void resolveDirectories(SessionSettings sessionSettings) {
        resolveDirectories(sessionSettings, null);
        stream(sessionSettings).forEach(sessionID -> {
            resolveDirectories(sessionSettings, sessionID);
        });
    }

    private void resolveDirectories(SessionSettings sessionSettings, SessionID sessionID) {
        boolean isDictionaryDefined = Objects.nonNull(sessionID) ?
                sessionSettings.isSetting(sessionID, DATA_DICTIONARY) :
                sessionSettings.isSetting(DATA_DICTIONARY);

        if (isDictionaryDefined) {
            try {
                String dataDictionaryLocation = Objects.nonNull(sessionID) ?
                        sessionSettings.getString(sessionID, DATA_DICTIONARY) :
                        sessionSettings.getString(DATA_DICTIONARY);

                String path = getPath(dataDictionaryLocation);

                if (Objects.nonNull(sessionID)) {
                    sessionSettings.setString(sessionID, DATA_DICTIONARY, path);
                } else {
                    sessionSettings.setString(DATA_DICTIONARY, path);
                }
            } catch (ConfigError e) {
                throw new QuickFixJConfigurationException("Failed to set DataDictionary location", e);
            }
        }
    }

    private String getPath(String dataDictionaryLocation) {
        Resource resource = resourceLoader.getResource(dataDictionaryLocation);
        try {
            Method getPath = resource.getClass().getMethod("getPath");
            return (String) getPath.invoke(resource);
        } catch (NoSuchMethodException e) {
            return dataDictionaryLocation;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new QuickFixJConfigurationException("Failed to set DataDictionary location " + resource, e);
        }
    }
}
