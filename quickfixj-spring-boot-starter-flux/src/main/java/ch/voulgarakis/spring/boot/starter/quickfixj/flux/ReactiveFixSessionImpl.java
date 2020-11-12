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
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.MessageSink;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ReactiveFixSessionImpl extends AbstractFixSession implements ReactiveFixSession {

    //--------------------------------------------------
    //-----------------------METRICS--------------------
    //--------------------------------------------------
    private Counter messagesReceived;
    private Counter messagesSent;
    private Counter rejections;

    //--------------------------------------------------
    //--------------------CONSTRUCTORS------------------
    //--------------------------------------------------

    /**
     * SessionID resolved by {@link ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager}.
     */
    public ReactiveFixSessionImpl() {
    }

    /**
     * @param sessionId Session Id manually assigned.
     */
    public ReactiveFixSessionImpl(SessionID sessionId) {
        super(sessionId);
    }

    //--------------------------------------------------
    //-----------------------METRICS--------------------
    //--------------------------------------------------
    @Autowired(required = false)
    public void setMeterRegistry(MeterRegistry meterRegistry) {
        String fixSessionName = FixSessionUtils.extractFixSessionName(this);
        if (StringUtils.isBlank(fixSessionName)) {
            return;
        }

        //The connection state
        Gauge.builder("quickfixj.flux.connection", () -> isLoggedOn() ? 1 : 0)
                .description("Connection state of reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .register(meterRegistry);

        //The number of subscribers
        Gauge.builder("quickfixj.flux.subscribers", this::sinkSize)
                .description("Number of subscribers on reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .register(meterRegistry);

        //The counters for FIX messages and FIX errors
        messagesReceived = Counter.builder("quickfixj.flux.messages.received")
                .description("Number of received FIX messages on reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .baseUnit("messages")
                .register(meterRegistry);
        messagesSent = Counter.builder("quickfixj.flux.messages.sent")
                .description("Number of sent FIX messages on reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .baseUnit("messages")
                .register(meterRegistry);
        rejections = Counter.builder("quickfixj.flux.rejections")
                .description("Number of received FIX rejections on reactive fix session")
                .tag("fixSessionName", fixSessionName)
                .baseUnit("rejects")
                .register(meterRegistry);

    }

    //--------------------------------------------------
    //-----------------FIX MESSAGE/ERROR----------------
    //--------------------------------------------------
    @Override
    protected void received(Message message) {
        if (Objects.nonNull(messagesReceived)) {
            messagesReceived.increment();
        }
        super.received(message);
    }

    @Override
    protected void error(SessionException ex) {
        if (Objects.nonNull(rejections)) {
            rejections.increment();
        }
        super.error(ex);
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
        FluxSink<Message> sink = processor.sink();

        //Create the underlying fix message sink
        MessageSink messageSink = createSink(messageSelector, sink::next, sink::error);
        //When sink is disposed (cancelled, terminated) we remove it from the sinks
        sink.onDispose(messageSink::dispose);

        //Return the flux
        return processor
                //If too many fix messages received that cannot be consumed in time, discard the oldest unprocessed messages
                .onBackpressureLatest();
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
                if (Objects.nonNull(messagesSent)) {
                    messagesSent.increment();
                }
                return Mono.just(message);
            } catch (SessionNotFound sessionNotFound) {
                if (Objects.nonNull(rejections)) {
                    rejections.increment();
                }
                return Mono.error(new QuickFixJException(sessionNotFound));
            }
        })
                //expose metrics if enabled
                .metrics();
    }

    @Override
    public Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier,
            Function<Message, RefIdSelector> refIdSelectorSupplier) {
        //Send the FIX request message
        return send(messageSupplier)
                //then
                .flatMapMany(message -> {
                    //This selector will associate FIX response messages received by the session, with this quote request.
                    RefIdSelector refIdSelector = refIdSelectorSupplier.apply(message);
                    //Subscribe to the responses relevant to this quote request
                    return subscribe(refIdSelector);
                })
                //expose metrics if enabled
                .metrics();
    }
}
