package ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingContext;
import reactor.util.context.Context;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public final class LoggingUtils {

    public static LoggingContext loggingContext(String key, String value) {
        return ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingUtils.loggingContext(key, value);
    }

    public static LoggingContext loggingContext(Map<String, String> context) {
        return ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingUtils.loggingContext(context);
    }

    public static LoggingContext loggingContext(Context context) {
        Map<String, String> ctx = context.stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        return loggingContext(ctx);
    }

    public static Runnable withLoggingContext(Map<String, String> context, Runnable runnable) {
        return ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingUtils.withLoggingContext(context, runnable);
    }

    public static <T> Callable<T> withLoggingContext(Map<String, String> context, Callable<T> callable) {
        return ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingUtils.withLoggingContext(context, callable);
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
