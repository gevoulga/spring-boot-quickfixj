package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import reactor.util.context.Context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public final class LoggingUtils {

    public static LoggingContext loggingContext(String key, String value) {
        return new LoggingContext(Collections.singletonMap(key, value));
    }

    public static LoggingContext loggingContext(Map<String, String> context) {
        return new LoggingContext(context);
    }

    public static LoggingContext loggingContext(Context context) {
        Map<String, String> ctx = context.stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        return new LoggingContext(ctx);
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

    public static Context withLoggingContext(LoggingContext loggingContext) {
        return withLoggingContext(loggingContext.getContext());
    }

    public static Context withLoggingContext(Map<String, String> context) {
        return context.entrySet().stream()
                .map(e -> Context.of(e.getKey(), e.getValue()))
                .reduce(Context::putAll)
                .orElse(Context.empty());
    }
}
