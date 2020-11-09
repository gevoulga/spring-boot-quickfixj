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

import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector;
import quickfix.FieldNotFound;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.StringField;
import quickfix.field.ClOrdID;
import quickfix.field.MDReqID;
import quickfix.field.QuoteReqID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.safeGetIdForRequest;

public abstract class ReactiveFixSessionMock implements ReactiveFixSession {

    //A registry of the open streams
    private final Map<String, Flux<Message>> streams = new HashMap<>();

    @Override
    public Flux<Message> subscribe(Predicate<Message> messageSelector) {
        return Flux.fromIterable(streams.values())
                .flatMap(quoteFlux -> quoteFlux)
                .filter(messageSelector);
    }

    @Override
    public Mono<Message> send(Supplier<Message> messageSupplier) {
        return Mono.defer(() -> {
            Message message = messageSupplier.get();
            safeGetIdForRequest(message)
                    .ifPresent(quoteReqId -> {
                        Flux<Message> quoteGenerator = quoteGenerator(quoteReqId, message);
                        if (Objects.nonNull(quoteGenerator)) {
                            streams.putIfAbsent(quoteReqId, quoteGenerator);
                        }
                    });
            return Mono.just(message);
        });
    }

    @Override
    public Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier) {
        return send(messageSupplier)
                .flatMapMany(message -> {
                    //Selector that will associate responses (referenceId) with the requestId
                    RefIdSelector refIdSelector = new RefIdSelector(message);
                    return subscribe(refIdSelector);
                });
    }

    @Override
    public Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier,
            Function<Message, RefIdSelector> refIdSelectorSupplier) {
        return send(messageSupplier)
                .flatMapMany(message -> {
                    RefIdSelector refIdSelector = refIdSelectorSupplier.apply(message);
                    return subscribe(refIdSelector);
                });
    }

    protected abstract Flux<Message> messageGenerator(String quoteReqId, Message request);

    private Flux<Message> quoteGenerator(String reqId, Message request) {
        Flux<Message> messageGenerator = messageGenerator(reqId, request);

        if (Objects.nonNull(messageGenerator)) {
            return messageGenerator
                    //Set the requestId
                    .map(message -> {
                        try {
                            StringField mdReqId = message.getField(new MDReqID(reqId));
                            mdReqId.setValue(reqId);
                            message.setField(mdReqId);
                        } catch (FieldNotFound fieldNotFound) {
                            //Nth to do
                        }
                        try {
                            StringField quoteReqId = message.getField(new QuoteReqID(reqId));
                            quoteReqId.setValue(reqId);
                            message.setField(quoteReqId);
                        } catch (FieldNotFound fieldNotFound) {
                            //Nth to do
                        }
                        try {
                            StringField clOrdId = message.getField(new ClOrdID(reqId));
                            clOrdId.setValue(reqId);
                            message.setField(clOrdId);
                        } catch (FieldNotFound fieldNotFound) {
                            //Nth to do
                        }
                        return message;
                    })
                    //Close when finished
                    .doOnTerminate(() -> streams.remove(reqId));
        } else {
            return null;
        }
    }


    @Override
    public boolean isLoggedOn() {
        return true;
    }

    @Override
    public SessionID getSessionId() {
        return null;
    }
}
