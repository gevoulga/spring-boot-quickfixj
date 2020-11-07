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

package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public class LoggingUtils {

    //This can be used in a logging framework to print the associated id in the log pattern
    public static final String ID = "id";

    public static LoggingContext loggingContext(String id) {
        return loggingContext(ID, id);
    }

    public static LoggingContext loggingContext() {
        return loggingContext(getContext());
    }

    public static LoggingContext loggingContext(String key, String value) {
        return new LoggingContext(Collections.singletonMap(key, value));
    }

    public static LoggingContext loggingContext(Map<String, String> context) {
        return new LoggingContext(context);
    }

    public static Runnable withLoggingContext(Runnable runnable) {
        return withLoggingContext(getContext(), runnable);
    }


    public static Runnable withLoggingContext(Map<String, String> context, Runnable runnable) {
        return () -> {
            // And this is called in the new thread already, so here we are updating id in the new context:
            try (LoggingContext ignored = loggingContext(context)) {
                runnable.run();
            }
        };
    }

    public static <T> Callable<T> withLoggingContext(Callable<T> callable) {
        return withLoggingContext(getContext(), callable);
    }

    public static <T> Callable<T> withLoggingContext(Map<String, String> context, Callable<T> callable) {
        return () -> {
            // And this is called in the new thread already, so here we are updating id in the new context:
            try (LoggingContext ignored = loggingContext(context)) {
                return callable.call();
            }
        };
    }

    protected static Map<String, String> getContext() {
        String id = MDC.get(ID);
        if (StringUtils.isNotBlank(id)) {
            return Collections.singletonMap(ID, id);
        } else {
            return Collections.emptyMap();
        }
    }
}
