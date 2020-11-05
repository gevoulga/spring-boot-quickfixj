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

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector;
import quickfix.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface ReactiveFixSession extends FixSessionInterface {
    /**
     * Subscribe to a stream of message in the Fix Session, with the specified scope-filter (selector).
     *
     * @param messageSelector the scope filter that will check which messages received are relevant to this subscription.
     * @return Flux of messages received from the fix session, that match the filter criteria.
     */
    Flux<Message> subscribe(Predicate<Message> messageSelector);

    /**
     * Send a message to the fix session.
     *
     * @param messageSupplier the message supplier that will be invoked when the sending will be executed.
     * @return a Mono that completes when the sending is done, or returns error if the sending failed.
     */
    Mono<Message> send(Supplier<Message> messageSupplier);

    /**
     * Convenient method that allows to send a message to the fix session and then subscribe to the response(s) received for this message.
     * The responses are associated with the requests based on the requestId tag in the FIX messages, using the {@link ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector}.
     *
     * @param messageSupplier the message supplier that will be invoked when the sending will be executed.
     * @return the Flux of fix messages received by the session associated with the request message, using {@link ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector}.
     */
    default Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier) {
        return sendAndSubscribe(messageSupplier, RefIdSelector::new);
    }

    /**
     * Convenient method that allows to send a message to the fix session and then subscribe to the response(s) received for this message.
     * The responses are associated with the requests based on the requestId tag in the FIX messages.
     *
     * @param messageSupplier       the message supplier that will be invoked when the sending will be executed.
     * @param refIdSelectorSupplier a RefIdSelector that will associate a request with a response.
     * @return the Flux of fix messages received by the session associated with the request message.
     */
    Flux<Message> sendAndSubscribe(Supplier<Message> messageSupplier,
                                   Function<Message, RefIdSelector> refIdSelectorSupplier);

}
