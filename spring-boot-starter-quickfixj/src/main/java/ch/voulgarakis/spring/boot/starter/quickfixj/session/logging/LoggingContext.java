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

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

public final class LoggingContext implements AutoCloseable {

    private final Map<String, String> newContext;
    private final Map<String, String> oldContext;

    LoggingContext(Map<String, String> context) {
        this.newContext = context;
        this.oldContext = setContext(context);
    }

    private static Map<String, String> setContext(Map<String, String> context) {
        Map<String, String> oldContext = new HashMap<>();
        for (String key : context.keySet()) {
            oldContext.putIfAbsent(key, MDC.get(key));
        }
        context.forEach(LoggingContext::setValue);
        return oldContext;
    }

    private static void setValue(String key, String value) {
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    public LoggingContext and(String key, String value) {
        String oldValue = MDC.get(key);
        oldContext.putIfAbsent(key, oldValue);
        newContext.put(key, value);
        setValue(key, value);
        return this;
    }

    public Map<String, String> getContext() {
        return newContext;
    }

    @Override
    public void close() {
        // resume logging context
        setContext(oldContext);
    }
}