package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public final class LoggingUtils {

    public static LoggingContext loggingContext(String key, String value) {
        return new LoggingContext(Collections.singletonMap(key, value));
    }

    public static LoggingContext loggingContext(Map<String, String> context) {
        return new LoggingContext(context);
    }

    public static Runnable withLoggingContext(Map<String, String> context, Runnable runnable) {
        return () -> {
            // And this is called in the new thread already, so here we are updating id in the new context:
            try (LoggingContext ignored = loggingContext(context)) {
                runnable.run();
            }
        };
    }

    public static <T> Callable<T> withLoggingContext(Map<String, String> context, Callable<T> callable) {
        return () -> {
            // And this is called in the new thread already, so here we are updating id in the new context:
            try (LoggingContext ignored = loggingContext(context)) {
                return callable.call();
            }
        };
    }
}
