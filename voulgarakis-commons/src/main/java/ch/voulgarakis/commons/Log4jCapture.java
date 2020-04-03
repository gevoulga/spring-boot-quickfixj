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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.util.LinkedList;
import java.util.List;

public class Log4jCapture extends AppenderSkeleton {
    private final List<LoggingEvent> log = new LinkedList<>();
    private final Level level;

    private Log4jCapture(Level level) {
        this.level = level;
    }

    public static Log4jCapture logCapture() {
        return new Log4jCapture(Level.ALL);
    }

    public static Log4jCapture logCapture(Level level) {
        return new Log4jCapture(level);
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        if (loggingEvent.getLevel().isGreaterOrEqual(level))
            log.add(loggingEvent);
    }

    @Override
    public void close() {
    }

    public List<LoggingEvent> getLog() {
        return log;
    }
}
