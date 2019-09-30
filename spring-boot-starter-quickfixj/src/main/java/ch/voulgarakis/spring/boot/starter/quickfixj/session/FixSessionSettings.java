package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJSettingsNotFoundException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ResourceCondition;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import quickfix.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.ensureUniqueSessionNames;
import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.tuple.Pair.of;

public class FixSessionSettings extends ResourceCondition {

    private static final Logger LOG = LoggerFactory.getLogger(FixSessionSettings.class);

    public static final String SYSTEM_VARIABLE_QUICKFIXJ_CONFIG = "quickfixj.config";
    private static final String QUICKFIXJ_CONFIG = "quickfixj.cfg";
    private static final String SESSION_NAME = "SessionName";
    private static final String CONNECTION_TYPE = "ConnectionType";
    private static final String CONCURRENT = "Concurrent";
    private static final String ACCEPTOR = "Acceptor";

    public FixSessionSettings() {
        super("QuickFixJ Server", SYSTEM_VARIABLE_QUICKFIXJ_CONFIG,
                "file:./" + QUICKFIXJ_CONFIG, "classpath:/" + QUICKFIXJ_CONFIG);
    }

    public static SessionSettings loadSettings(String userDefinedLocation) {
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
                    SessionSettings sessionSettings = new SessionSettings(resource.get().getInputStream());
                    ensureUniqueSessionNames(sessionSettings);
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
        if (resource.exists())
            return Optional.of(resource);
        else if (failIfNotFound)
            throw new QuickFixJSettingsNotFoundException("Resource not found: " + location);
        else
            return Optional.empty();
    }

    public static Connector createConnector(Application application, MessageStoreFactory messageStoreFactory, SessionSettings sessionSettings, LogFactory logFactory, MessageFactory messageFactory) throws ConfigError, FieldConvertError {
        if (sessionSettings.getString(CONNECTION_TYPE).equalsIgnoreCase(ACCEPTOR)) {
            if (sessionSettings.isSetting(CONCURRENT) && sessionSettings.getBool(CONCURRENT)) {
                return new ThreadedSocketAcceptor(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            } else {
                return new SocketAcceptor(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            }
        } else {
            if (sessionSettings.isSetting(CONCURRENT) && sessionSettings.getBool(CONCURRENT)) {
                return new ThreadedSocketInitiator(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            } else {
                return new SocketInitiator(application, messageStoreFactory, sessionSettings, logFactory,
                        messageFactory);
            }
        }
    }

    static String extractSessionName(SessionSettings sessionSettings, SessionID sessionID) {
        try {
            if (sessionSettings.isSetting(sessionID, SESSION_NAME))
                return sessionSettings.getString(sessionID, SESSION_NAME);
            else
                return sessionID.toString();
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException("Failed to get SessionName from properties.", configError);
        }
    }
}
