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

package ch.voulgarakis.commons;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.LinkedList;
import java.util.List;

public class LogbackCapture extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> log = new LinkedList<>();
    private final Level level;

    private LogbackCapture(Level level) {
        this.level = level;
    }

    public static LogbackCapture logCapture() {
        return new LogbackCapture(Level.ALL);
    }

    public static LogbackCapture logCapture(Level level) {
        return new LogbackCapture(level);
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (loggingEvent.getLevel().isGreaterOrEqual(level)) {
            log.add(loggingEvent);
        }
    }

    public List<ILoggingEvent> getLog() {
        return log;
    }
}