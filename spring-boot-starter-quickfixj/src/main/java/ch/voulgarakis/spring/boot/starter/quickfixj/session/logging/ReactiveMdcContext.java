package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import org.reactivestreams.Subscription;
import org.springframework.lang.NonNull;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * Helper that copies the state of Reactor [Context] to MDC on the #onNext function.
 */
public class ReactiveMdcContext<T> implements CoreSubscriber<T> {

    private final CoreSubscriber<T> coreSubscriber;

    ReactiveMdcContext(CoreSubscriber<T> coreSubscriber) {
        this.coreSubscriber = coreSubscriber;
    }

    @Override
    @NonNull
    public Context currentContext() {
        return coreSubscriber.currentContext();
    }

    private void setLoggingContext(Runnable runnable) {
        try (LoggingContext ignored = LoggingUtils.loggingContext(currentContext())) {
            runnable.run();
        }
    }

    @Override
    public void onSubscribe(Subscription s) {
        setLoggingContext(() -> coreSubscriber.onSubscribe(s));
    }

    @Override
    public void onNext(T t) {
        setLoggingContext(() -> coreSubscriber.onNext(t));
    }

    @Override
    public void onError(Throwable throwable) {
        setLoggingContext(() -> coreSubscriber.onError(throwable));
    }

    @Override
    public void onComplete() {
        setLoggingContext(coreSubscriber::onComplete);
    }
}