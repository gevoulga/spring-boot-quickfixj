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
        if (loggingEvent.getLevel().isGreaterOrEqual(level))
            log.add(loggingEvent);
    }

    public List<ILoggingEvent> getLog() {
        return log;
    }
}