package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionNotFound;
import quickfix.field.MsgType;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ReactiveAbstractFixSession extends AbstractFixSession implements ReactiveFixSession {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveAbstractFixSession.class);

    private final List<Tuple2<Predicate<Message>, FluxSink<Message>>> sinks =
            new CopyOnWriteArrayList<>();
    private final AtomicReference<SessionDroppedException> loggedOut = new AtomicReference<>();

    //--------------------------------------------------
    //-----------------FIX MESSAGE/ERROR----------------
    //--------------------------------------------------
    @Override
    protected void received(Message message) {
        loggedOut(null);
        notifySubscribers(message, sink -> sink.next(message), false);
    }

    @Override
    protected void error(SessionException ex) {
        loggedOut(ex);
        notifySubscribers(ex.getFixMessage(), sink -> sink.error(ex), true);
    }

    /**
     * Store the logout (if not null) so subsequent subscriptions will be notified that the session has been dropped.
     *
     * @param ex the session exception.
     *           If of type SessionDroppedException, it will be stored.
     *           Otherwise, any existing error will be cleared.
     */
    private void loggedOut(SessionException ex) {
        if (ex instanceof SessionDroppedException) {
            SessionDroppedException droppedException = (SessionDroppedException) ex;
            //Do not override if a SessionDroppedException with a Fix Message already exists!
            if (Objects.nonNull(ex.getFixMessage())) {
                loggedOut.set(droppedException);
            } else {
                loggedOut.compareAndSet(null, droppedException);
            }
        } else {
            loggedOut.set(null);
        }
    }

    private synchronized void notifySubscribers(Message message, Consumer<FluxSink<Message>> sinkConsumer,
                                                boolean notifyAndDrop) {

        //Notify all the sinks in parallel
        List<Tuple2<Predicate<Message>, FluxSink<Message>>> notifiedSinks =
                sinks.parallelStream()
                        .flatMap(tuple -> {
                            //Check if we should notify the subscriber (based on the predicate of the sink)
                            boolean notifySubscribers = tuple.getT1().test(message);
                            if (notifySubscribers) {
                                //Notify the sink
                                sinkConsumer.accept(tuple.getT2());
                            }
                            //Return the notified sink
                            return notifySubscribers ? Stream.of(tuple) : Stream.empty();
                        })
                        .collect(Collectors.toList());

        //Remove all the notified/cancelled sinks
        if (notifyAndDrop) {
            sinks.removeAll(notifiedSinks);
        }
        //Remove any cancelled sinks
        sinks.removeAll(
                sinks.parallelStream()
                        .filter(t -> t.getT2().isCancelled())
                        .collect(Collectors.toList())
        );

        //Log a warning if nobody was notified
        if (Objects.nonNull(message) && notifiedSinks.isEmpty() && !FixMessageUtils.isMessageOfType(message, MsgType.LOGOUT)) {
            LOG.warn("Message received could not be associated with any Request. Message: {}", message);
        }
    }

    //--------------------------------------------------
    //-----------------FIX Flux<Message>----------------
    //--------------------------------------------------

    /**
     * Subscribe to the messages received from FIX.
     *
     * @param messageSelector the filter that selects the relevant messages for this stream.
     * @return a Flux<Messages> with the FIX messages that are relevant
     */
    @Override
    public Flux<Message> subscribe(Predicate<Message> messageSelector) {
        //UnicastProcessor<Message> processor = UnicastProcessor.create();
        //DirectProcessor<Message> processor = DirectProcessor.create();
        EmitterProcessor<Message> processor = EmitterProcessor.create();
        //WorkQueueProcessor<Message> processor = WorkQueueProcessor.create();
        FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.LATEST);
        sinks.add(Tuples.of(messageSelector, sink));

        //Notify new subscriber if session has been dropped
        SessionDroppedException sessionDroppedException = loggedOut.get();
        if (Objects.nonNull(sessionDroppedException)) {
            sink.error(sessionDroppedException);
        }
        return processor.onBackpressureLatest();
    }

    //--------------------------------------------------
    //-----------------SEND FIX MESSAGE-----------------
    //--------------------------------------------------
    @Override
    public Mono<Message> send(Message message) {
        return Mono.defer(() -> {
            try {
                Session.sendToTarget(message, getSessionId());
                return Mono.just(message);
            } catch (SessionNotFound sessionNotFound) {
                return Mono.error(new QuickFixJException(sessionNotFound));
            }
        });
    }
}
