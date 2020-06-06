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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionDroppedException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class ReactiveAbstractFixSession extends AbstractFixSession implements ReactiveFixSession {

    private static final Logger LOG = LoggerFactory.getLogger(ReactiveAbstractFixSession.class);

    private final Set<Tuple2<Predicate<Message>, FluxSink<Message>>> sinks =
            ConcurrentHashMap.newKeySet();
    private final AtomicReference<SessionDroppedException> loggedOut = new AtomicReference<>();

    //--------------------------------------------------
    //-----------------------METRICS--------------------
    //--------------------------------------------------
    @Autowired(required = false)
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        String fixSessionName = FixSessionUtils.extractFixSessionName(this);
        Gauge.builder("quickfixj.flux.subscribers", sinks, Collection::size)
                .description("Number of subscribers on reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .register(meterRegistry);
    }

    //--------------------------------------------------
    //-----------------FIX MESSAGE/ERROR----------------
    //--------------------------------------------------
    @Override
    protected void received(Message message) {
        loggedOut(null);
        notifySubscribers(message, sink -> sink.next(message));
    }

    @Override
    protected void error(SessionException ex) {
        loggedOut(ex);
        notifySubscribers(ex.getFixMessage(), sink -> sink.error(ex));
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

    private synchronized void notifySubscribers(Message message, Consumer<FluxSink<Message>> sinkConsumer) {
        //Notify all the sinks in parallel
        int notifiedSinks = sinks.parallelStream()
                .mapToInt(tuple -> {
                    //Check if we should notify the subscriber (based on the predicate of the sink)
                    boolean notifySubscribers = tuple.getT1().test(message);
                    if (notifySubscribers) {
                        //Notify the sink
                        sinkConsumer.accept(tuple.getT2());
                    }
                    //Return the notified sink
                    return notifySubscribers ? 1 : 0;
                })
                .sum();

        //Log a warning if nobody was notified
        if (Objects.nonNull(message) && notifiedSinks == 0 && !FixMessageUtils.isMessageOfType(message, MsgType.LOGOUT)) {
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

        //Create the sink and tie it with the scope-filter
        FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.LATEST);
        Tuple2<Predicate<Message>, FluxSink<Message>> sinkTuple = Tuples.of(messageSelector, sink);

        //When sink is disposed (cancelled, terminated) we remove it from the sinks
        sink.onDispose(() -> sinks.remove(sinkTuple));
        //Add the sink
        sinks.add(sinkTuple);

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
    public Mono<Message> send(Supplier<Message> messageSupplier) {
        return Mono.defer(() -> {
            try {
                Message message = messageSupplier.get();
                Session.sendToTarget(message, getSessionId());
                return Mono.just(message);
            } catch (SessionNotFound sessionNotFound) {
                return Mono.error(new QuickFixJException(sessionNotFound));
            }
        });
    }

    @Override
    public Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier, Function<Message, RefIdSelector> refIdSelectorSupplier) {
        //Send the FIX request message
        return send(messageSupplier)
                //then
                .flatMapMany(message -> {
                    //This selector will associate FIX response messages received by the session, with this quote request.
                    RefIdSelector refIdSelector = refIdSelectorSupplier.apply(message);
                    //Subscribe to the responses relevant to this quote request
                    return subscribe(refIdSelector);
                });
    }
}
