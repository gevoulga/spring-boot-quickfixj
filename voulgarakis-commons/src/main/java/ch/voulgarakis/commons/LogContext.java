package ch.voulgarakis.commons;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.MDC;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LogContext implements AutoCloseable {
    private final Map<String, Object> oldContext;

    private LogContext(Map<Object, Object> context) {
        //Set the MDC context, and store any previous entries
        oldContext = context.entrySet().parallelStream()
                .map(e -> {
                    String key = Objects.toString(e.getKey());
                    Object oldEntry = MDC.get(key);
                    MDC.put(key, e.getValue());
                    return Pair.of(key, oldEntry);
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    public static LogContextBuilder builder() {
        return new LogContextBuilder();
    }

    public static LogContext fromMdcContext(Context context) {
        return new LogContext(context.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    @Override
    public void close() {
        //Restore MDC context
        oldContext.entrySet().parallelStream()
                .forEach(e -> {
                    String key = e.getKey();
                    Object oldValue = e.getValue();
                    if (Objects.isNull(oldValue))
                        MDC.remove(key);
                    else
                        MDC.put(key, oldValue);
                });
    }

    public static class LogContextBuilder {
        private final Map<Object, Object> contextEntries = new HashMap<>();

        private LogContextBuilder() {
        }

        public LogContextBuilder set(Object key, Object value) {
            if (!contextEntries.containsKey(key) && !contextEntries.containsValue(value)) {
                contextEntries.put(key, value);
            } else {
                throw new IllegalStateException("context entries must be unique");
            }
            return this;
        }

        public LogContext build() {
            return new LogContext(contextEntries);
        }

        public Context mdcContext() {
            return contextEntries.entrySet().parallelStream()
                    .map(e -> Context.of(e.getKey(), e.getValue()))
                    .reduce(Context::putAll)
                    .orElseGet(Context::empty);
        }
    }

}
