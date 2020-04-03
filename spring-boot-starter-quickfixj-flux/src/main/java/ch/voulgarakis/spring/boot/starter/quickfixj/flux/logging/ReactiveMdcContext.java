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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging;

import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingContext;
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