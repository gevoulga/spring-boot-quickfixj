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

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import quickfix.ConfigError;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.stream;
import static java.lang.Thread.currentThread;

public class FixSessionSettings {

    private static final Logger LOG = LoggerFactory.getLogger(FixSessionSettings.class);

    public static final String SYSTEM_VARIABLE_QUICKFIXJ_CONFIG = "quickfixj.config";
    public static final String QUICKFIXJ_CONFIG = "quickfixj.cfg";
    private static final String DATA_DICTIONARY = "DataDictionary";
    private static final String SESSION_NAME = "SessionName";

    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final Resource quickfixjConfig;

    public FixSessionSettings(Environment environment, ResourceLoader resourceLoader,
            String definedQuickfixjConfig) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.quickfixjConfig = findQuickfixjConfig(definedQuickfixjConfig);
    }

    public static Resource findQuickfixjConfig(String userDefinedLocation) {
        String[] locations = new String[]{
                userDefinedLocation,
                System.getProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG),
                "file:./" + QUICKFIXJ_CONFIG,
                "classpath:/" + QUICKFIXJ_CONFIG
        };
        for (int i = 0; i < locations.length; i++) {
            String location = locations[i];
            if (StringUtils.isBlank(location)) {
                continue;
            }
            ClassLoader classLoader = currentThread().getContextClassLoader();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
            Resource resource = resolver.getResource(location);
            if (resource.exists()) {
                LOG.info("Configuring QuickFixJ engine using file: {}", resource.getDescription());
                return resource;
            } else if (i < 2) {
                throw new QuickFixJSettingsNotFoundException(
                        "QuickFixJ configuration file not found at specified location: " + location);
            }
        }
        throw new QuickFixJSettingsNotFoundException(
                "QuickFixJ configuration file not found on any of the locations: " + Arrays.toString(locations));
    }

    public SessionSettings createSessionSettings() throws ConfigError, IOException {
        try (InputStream inputStream = quickfixjConfig.getInputStream()) {
            //SessionSettings sessionSettings = new SessionSettings(inputStream);
            InputStream stream;

            //Replace placeholders
            if (Objects.nonNull(environment)) {
                Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                String sessionSettingsAsString = s.hasNext() ? s.next() : "";
                String replacedPropertiesSessionSettingsString = environment.resolvePlaceholders(
                        sessionSettingsAsString);
                stream = new ByteArrayInputStream(
                        replacedPropertiesSessionSettingsString.getBytes());
            } else {
                stream = inputStream;
            }

            SessionSettings sessionSettings = new SessionSettings(stream);
            //Replace placeholders
            sessionSettings.setVariableValues(new Properties() {
                @Override
                public String getProperty(String key) {
                    return environment.getProperty(key);
                }
            });
            //Replace the directories specified to a format understood by quickfixj
            resolveDirectories(sessionSettings);

            //Make sure the session names are unique
            FixSessionUtils.ensureUniqueSessionNames(sessionSettings);

            return sessionSettings;
        }
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

    public static String extractSessionName(SessionSettings sessionSettings, SessionID sessionID) {
        try {
            if (sessionSettings.isSetting(sessionID, SESSION_NAME)) {
                return sessionSettings.getString(sessionID, SESSION_NAME);
            } else {
                return sessionID.toString();
            }
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException("Failed to get SessionName from properties.", configError);
        }
    }

    public static SessionID sessionID(SessionSettings sessionSettings, String sessionName) {
        List<SessionID> sessionIds = stream(sessionSettings)
                .filter(sessionID -> {
                    String extractedSessionName = extractSessionName(sessionSettings, sessionID);
                    return StringUtils.equals(extractedSessionName, sessionName);
                })
                .collect(Collectors.toList());

        if (sessionIds.isEmpty()) {
            throw new QuickFixJConfigurationException("No session id found");
        } else if (sessionIds.size() > 1) {
            throw new QuickFixJConfigurationException("Too many sessionIds found: " + sessionIds);
        }
        return sessionIds.get(0);
    }
}
