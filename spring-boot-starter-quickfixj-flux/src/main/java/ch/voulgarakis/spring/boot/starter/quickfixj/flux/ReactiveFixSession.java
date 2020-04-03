package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import quickfix.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

public interface ReactiveFixSession extends FixSessionInterface {

    Flux<Message> subscribe(Predicate<Message> messageSelector);

    Mono<Message> send(Message message);
}
