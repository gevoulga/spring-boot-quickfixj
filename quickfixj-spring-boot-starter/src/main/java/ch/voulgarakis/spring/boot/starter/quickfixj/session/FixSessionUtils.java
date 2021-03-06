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

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionMapping;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.fix.session.FixSession;
import org.springframework.beans.factory.NamedBean;
import quickfix.SessionID;
import quickfix.SessionSettings;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.extractSessionName;

public class FixSessionUtils {

    public static String extractFixSessionName(AbstractFixSession fixSession) {
        FixSessionMapping[] annotationsByType = fixSession.getClass().getAnnotationsByType(FixSessionMapping.class);
        if (annotationsByType.length > 1) {
            throw new QuickFixJConfigurationException("Only one @FixSessionMapping(...) is allowed.");
        } else if (annotationsByType.length == 1) {
            return annotationsByType[0].value();
        } else {
            if (NamedBean.class.isAssignableFrom(fixSession.getClass())) {
                return ((NamedBean) fixSession).getBeanName();
            } else {
                throw new QuickFixJConfigurationException(
                        fixSession.getClass().getSimpleName()
                                + " needs to be associated with a SessionId. Either use @FixSessionMapping on your "
                                + fixSession.getClass().getSimpleName()
                                + ", or make your " + fixSession.getClass().getSimpleName()
                                + " implement a NamedBean. "
                                + fixSession.getClass().getSimpleName()
                                + ": " + fixSession);
            }
        }
    }

    public static Stream<SessionID> stream(SessionSettings sessionSettings) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(sessionSettings.sectionIterator(), Spliterator.NONNULL),
                false);
    }

    //////////////////////////////
    //// Unique session names ////
    //////////////////////////////
    public static void ensureUniqueSessionNames(SessionSettings sessionSettings) {
        if (sessionSettings.size() > 1) {
            List<String> sessionNames = stream(sessionSettings)
                    .map(sessionID -> extractSessionName(sessionSettings, sessionID))
                    .collect(Collectors.toList());
            ensureUniqueSessionNames(sessionNames,
                    "Property must be unique in " + SessionSettings.class.getSimpleName());
        }
    }

    public static void ensureUniqueSessionNames(Collection<? extends AbstractFixSession> sessionBeans) {
        if (sessionBeans.size() > 1) {
            List<String> sessionNames = sessionBeans.stream()
                    .map(FixSessionUtils::extractFixSessionName)
                    .collect(Collectors.toList());
            FixSessionUtils.ensureUniqueSessionNames(sessionNames,
                    "Multiple " + FixSession.class.getSimpleName() + " beans specified for the same session name.");
        }
//        //Make sure there's a session mapping to session settings
//        else if (sessionBeans.isEmpty()) {
//            throw new QuickFixJConfigurationException("No session found in quickfixj session settings.");
//        }
    }

    private static void ensureUniqueSessionNames(List<String> sessionNames, String errorMessage) {
        if (sessionNames.size() > 1) {
            List<String> duplicateSessionNames = sessionNames.stream()
                    .filter(sessionName -> Collections.frequency(sessionNames, sessionName) > 1)
                    .collect(Collectors.toList());
            if (!duplicateSessionNames.isEmpty()) {
                throw new QuickFixJConfigurationException(
                        String.format(errorMessage + ". [SessionName/SessionId] Found: %s in %s", duplicateSessionNames,
                                sessionNames));
            }
        }
    }
}
