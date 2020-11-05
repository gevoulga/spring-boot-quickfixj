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
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import quickfix.Dictionary;
import quickfix.*;
import quickfix.field.Password;
import quickfix.field.Username;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.stream;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.tuple.Pair.of;
import static quickfix.SessionID.NOT_SET;
import static quickfix.SessionSettings.*;

public class FixSessionSettings extends ResourceCondition {

    public static final String SYSTEM_VARIABLE_QUICKFIXJ_CONFIG = "quickfixj.config";
    private static final Logger LOG = LoggerFactory.getLogger(FixSessionSettings.class);
    private static final String QUICKFIXJ_CONFIG = "quickfixj.cfg";
    private static final String DATA_DICTIONARY = "DataDictionary";
    private static final String SESSION_NAME = "SessionName";
    private static final String USERNAME = "Username";
    private static final String PASSWORD = "Password";

    public FixSessionSettings() {
        super("QuickFixJ Server", SYSTEM_VARIABLE_QUICKFIXJ_CONFIG,
                "file:./" + QUICKFIXJ_CONFIG, "classpath:/" + QUICKFIXJ_CONFIG);
    }

    public static SessionSettings loadSettings(String userDefinedLocation, Environment environment,
                                               ResourceLoader resourceLoader) {
        List<Pair<String, Boolean>> locations = Stream.of(of(userDefinedLocation, true),
                of(System.getProperty(SYSTEM_VARIABLE_QUICKFIXJ_CONFIG), true),
                of("file:./" + QUICKFIXJ_CONFIG, false),
                of("classpath:/" + QUICKFIXJ_CONFIG, false))
                .collect(Collectors.toList());

        try {
            for (Pair<String, Boolean> location : locations) {
                Optional<Resource> resource = loadResource(location.getLeft(), location.getRight());
                if (resource.isPresent()) {
                    LOG.info("Loading settings from '{}'", location);
                    SessionSettings sessionSettings = createSessionSettings(environment, resourceLoader,
                            resource.get());
                    FixSessionUtils.ensureUniqueSessionNames(sessionSettings);
                    return sessionSettings;
                }
            }
            throw new QuickFixJSettingsNotFoundException("Settings file not found");
        } catch (ConfigError | IOException e) {
            throw new QuickFixJSettingsNotFoundException(e.getMessage(), e);
        }
    }


    private static Optional<Resource> loadResource(String location, boolean failIfNotFound) {
        if (location == null) {
            return empty();
        }

        ClassLoader classLoader = currentThread().getContextClassLoader();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);

        Resource resource = resolver.getResource(location);
        if (resource.exists()) {
            return Optional.of(resource);
        } else if (failIfNotFound) {
            throw new QuickFixJSettingsNotFoundException("Resource not found: " + location);
        } else {
            return Optional.empty();
        }
    }

    private static SessionSettings createSessionSettings(Environment environment, ResourceLoader resourceLoader,
                                                         Resource resource) throws ConfigError, IOException {
        try (InputStream inputStream = resource.getInputStream()) {
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
                stream = resource.getInputStream();
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
            resolveDirectories(sessionSettings, resourceLoader);
            return sessionSettings;
        }
    }

    private static void resolveDirectories(SessionSettings sessionSettings, ResourceLoader resourceLoader) {
        resolveDirectories(sessionSettings, resourceLoader, null);
        stream(sessionSettings).forEach(sessionID -> {
            resolveDirectories(sessionSettings, resourceLoader, sessionID);
        });
    }

    private static void resolveDirectories(SessionSettings sessionSettings, ResourceLoader resourceLoader,
                                           SessionID sessionID) {
        boolean isDictionaryDefined = Objects.nonNull(sessionID) ?
                sessionSettings.isSetting(sessionID, DATA_DICTIONARY) :
                sessionSettings.isSetting(DATA_DICTIONARY);

        if (isDictionaryDefined) {
            try {
                String dataDictionaryLocation = Objects.nonNull(sessionID) ?
                        sessionSettings.getString(sessionID, DATA_DICTIONARY) :
                        sessionSettings.getString(DATA_DICTIONARY);

                String path = getPath(dataDictionaryLocation, resourceLoader);

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

    private static String getPath(String dataDictionaryLocation, ResourceLoader resourceLoader) {
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

    public static Connector createConnector(Application application, FixConnectionType fixConnectionType,
                                            MessageStoreFactory messageStoreFactory, SessionSettings sessionSettings,
                                            LogFactory logFactory, MessageFactory messageFactory) throws ConfigError {
        return fixConnectionType
                .createConnector(application, messageStoreFactory, sessionSettings, logFactory, messageFactory);
    }

    static String extractSessionName(SessionSettings sessionSettings, SessionID sessionID) {
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

    public static SessionID sessionID(SessionSettings sessionSettings, Dictionary dictionary) {
        Properties properties = sessionSettings.getDefaultProperties();
        properties.putAll(dictionary.toMap());
        return new SessionID(properties.getProperty(BEGINSTRING, NOT_SET),
                properties.getProperty(SENDERCOMPID, NOT_SET),
                properties.getProperty(SENDERSUBID, NOT_SET),
                properties.getProperty(SENDERLOCID, NOT_SET),
                properties.getProperty(TARGETCOMPID, NOT_SET),
                properties.getProperty(TARGETSUBID, NOT_SET),
                properties.getProperty(TARGETLOCID, NOT_SET),
                properties.getProperty(SESSION_QUALIFIER, NOT_SET));
    }

    public static Message authenticate(SessionSettings sessionSettings, SessionID sessionID, Message message) {
        try {
            String username = sessionSettings.getString(sessionID, USERNAME);
            message.setField(new Username(username));
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException("Failed to get Username from properties.", configError);
        }
        try {
            String password = sessionSettings.getString(sessionID, PASSWORD);
            message.setField(new Password(password));
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException("Failed to get Password from properties.", configError);
        }
        return message;
    }
}
