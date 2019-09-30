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

    public static Log4jCapture logCapture() {
        return new Log4jCapture(Level.ALL);
    }

    public static Log4jCapture logCapture(Level level) {
        return new Log4jCapture(level);
    }
}
