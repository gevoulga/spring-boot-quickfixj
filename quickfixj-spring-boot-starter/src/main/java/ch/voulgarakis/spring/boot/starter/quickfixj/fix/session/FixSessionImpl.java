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

package ch.voulgarakis.spring.boot.starter.quickfixj.fix.session;

import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJException;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager;
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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class FixSessionImpl extends AbstractFixSession implements FixSession {

    //--------------------------------------------------
    //-----------------------METRICS--------------------
    //--------------------------------------------------
    private Counter messagesReceived;
    private Counter messagesSent;
    private Counter rejections;

    /**
     * SessionID resolved by {@link FixSessionManager}.
     */
    public FixSessionImpl() {
    }

    /**
     * @param sessionId Session Id manually assigned.
     */
    public FixSessionImpl(SessionID sessionId) {
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
        Gauge.builder("quickfixj.connection", () -> isLoggedOn() ? 1 : 0)
                .description("Connection state of fix session")
                .tag("fixSessionName", fixSessionName)
                .register(meterRegistry);

        //The number of subscribers
        Gauge.builder("quickfixj.subscribers", this::sinkSize)
                .description("Number of subscribers on fix session")
                .tag("fixSessionName", fixSessionName)
                .register(meterRegistry);

        //The counters for FIX messages and FIX errors
        messagesReceived = Counter.builder("quickfixj.messages.received")
                .description("Number of received FIX messages on fix session")
                .tag("fixSessionName", fixSessionName)
                .baseUnit("messages")
                .register(meterRegistry);
        messagesSent = Counter.builder("quickfixj.messages.sent")
                .description("Number of sent FIX messages on fix session")
                .tag("fixSessionName", fixSessionName)
                .baseUnit("messages")
                .register(meterRegistry);
        rejections = Counter.builder("quickfixj.rejections")
                .description("Number of received FIX rejections on fix session")
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
    //-------------------FIX Callbacks------------------
    //--------------------------------------------------

    @Override
    public Disposable subscribe(Predicate<Message> messageSelector, Consumer<Message> onResponse,
            Consumer<Throwable> onError) {
        //Create the underlying fix message sink
        MessageSink messageSink = createSink(messageSelector, onResponse, onError);

        //When connection is disposed (cancelled, terminated) we remove it from the sinks
        return messageSink::dispose;
    }

    //--------------------------------------------------
    //-----------------SEND FIX MESSAGE-----------------
    //--------------------------------------------------
    @Override
    public Message send(Message message) {
        try {
            Session.sendToTarget(message, getSessionId());
            if (Objects.nonNull(messagesSent)) {
                messagesSent.increment();
            }
            return message;
        } catch (SessionNotFound sessionNotFound) {
            if (Objects.nonNull(rejections)) {
                rejections.increment();
            }
            throw new QuickFixJException(sessionNotFound);
        }
    }

    @Override
    public Disposable sendAndSubscribe(Message message, Function<Message, RefIdSelector> refIdSelectorSupplier,
            Consumer<Message> onResponse, Consumer<Throwable> onError) {
        //Send the FIX request message
        Message messageSent = send(message);

        //This selector will associate FIX response messages received by the session, with this quote request.
        RefIdSelector refIdSelector = refIdSelectorSupplier.apply(messageSent);

        //Subscribe to the responses relevant to this quote request
        return subscribe(refIdSelector, onResponse, onError);
    }
}
