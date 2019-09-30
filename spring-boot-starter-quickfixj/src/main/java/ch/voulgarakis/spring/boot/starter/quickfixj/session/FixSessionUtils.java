package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionMapping;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.NamedBean;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;

public class FixSessionUtils {

    static ImmutablePair<SessionID, FixSession> getFixSession(SessionSettings sessionSettings, List<FixSession> sessions, SessionID sessionID) {
        if (sessionSettings.size() == 1 && sessions.size() == 1) {
            return ImmutablePair.of(sessionID, sessions.get(0));
        } else {
            Map<String, FixSession> fixSessionMap = sessions.stream()
                    .map(fs -> Pair.of(extractFixSessionName(fs), fs))
                    .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
            String sessionName = extractSessionName(sessionSettings, sessionID);
            FixSession fixSession = fixSessionMap.entrySet().stream()
                    .filter(e -> StringUtils.equalsIgnoreCase(sessionName, e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> {
                        List<String> sessionNames = sessions.stream()
                                .map(FixSessionUtils::extractFixSessionName)
                                .collect(Collectors.toList());
                        return new QuickFixJConfigurationException(String.format("No bean found for SessionName.%s=[%s] in defined %s beans %s", SessionSettings.class.getSimpleName(), sessionName, FixSession.class.getSimpleName(), sessionNames));
                    });
            fixSession.setSessionId(sessionID);
            return ImmutablePair.of(sessionID, fixSession);
        }
    }

    static String extractFixSessionName(FixSession fixSession) {
        FixSessionMapping[] annotationsByType = fixSession.getClass().getAnnotationsByType(FixSessionMapping.class);
        if (annotationsByType.length > 1) {
            throw new QuickFixJConfigurationException("Only one @FixSessionMapping(...) is allowed.");
        } else if (annotationsByType.length == 1) {
            return annotationsByType[0].value();
        } else {
            if (NamedBean.class.isAssignableFrom(fixSession.getClass())) {
                return ((NamedBean) fixSession).getBeanName();
            } else {
                throw new QuickFixJConfigurationException("FixSession needs to be associated with a SessionId. Either use @FixSessionMapping on your FixSession, or make your FixSession implement a NamedBean. FixSession: " + fixSession);
            }
        }
    }

    static Stream<SessionID> stream(SessionSettings sessionSettings) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sessionSettings.sectionIterator(), Spliterator.NONNULL),
                false);
    }

    static void ensureUniqueSessionNames(SessionSettings sessionSettings) {
        if (sessionSettings.size() > 1) {
            List<String> sessionNames = stream(sessionSettings)
                    .map(sessionID -> extractSessionName(sessionSettings, sessionID))
                    .collect(Collectors.toList());
            ensureUniqueSessionNames(sessionNames,
                    "Property must be unique in " + SessionSettings.class.getSimpleName());
        }
    }

    static void ensureUniqueSessionNames(List<String> sessionNames, String errorMessage) {
        if (sessionNames.size() > 1) {
            List<String> duplicateSessionNames = sessionNames.stream()
                    .filter(sessionName -> Collections.frequency(sessionNames, sessionName) > 1)
                    .collect(Collectors.toList());
            if (!duplicateSessionNames.isEmpty()) {
                throw new QuickFixJConfigurationException(String.format(errorMessage + ". [SessionName/SessionId] Found: %s in %s", duplicateSessionNames, sessionNames));
            }
        }
    }
}
