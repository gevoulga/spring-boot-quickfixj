package ch.voulgarakis.spring.boot.starter.quickfixj.session.logging;

import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class ReactiveMdcContextConfiguration {

    public static final String LOGGING_CONTEXT = "LoggingContext";

    @PostConstruct
    public void contextOperatorHook() {
        Hooks.onEachOperator(LOGGING_CONTEXT,
                Operators.lift((scannable, coreSubscriber) -> new ReactiveMdcContext<>(coreSubscriber)));
    }

    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(LOGGING_CONTEXT);
    }

}
