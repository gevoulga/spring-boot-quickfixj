package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static reactor.util.function.Tuples.of;

public abstract class RxFixSession extends FixSession {

    private static final Logger LOG = LoggerFactory.getLogger(RxFixSession.class);
    private final List<Tuple2<Predicate<Message>, FluxSink<Message>>> sinks = new CopyOnWriteArrayList<>();

    @Override
    protected void received(Message message) {
        notifySusbcribers(message, sink -> sink.next(message));
    }

    @Override
    protected void error(RejectException ex) {
        notifySusbcribers(ex.getFixMessage(), sink -> sink.error(ex));
    }

    public Flux<Message> subscribe(Predicate<Message> messageSelector, Runnable onCancel) {
        UnicastProcessor<Message> processor = UnicastProcessor.create();
        FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.LATEST);
        sinks.add(of(messageSelector, sink));
        return processor.doOnCancel(onCancel);
    }

    private void notifySusbcribers(Message message, Consumer<FluxSink<Message>> sinkConsumer) {
        long notifiedSinks = sinks.parallelStream().mapToInt(tuple -> {
            boolean notifySubscribers = tuple.getT1().test(message);
            if (notifySubscribers) {
                sinkConsumer.accept(tuple.getT2());
            }
            return notifySubscribers ? 1 : 0;
        }).count();
        if (notifiedSinks == 0) {
            LOG.warn("Message received could not be associated with any Request. Message: {}", message);
        }
    }
}
