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

import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector;
import quickfix.Message;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface FixSession extends FixSessionInterface {

    /**
     * Send a message to the fix session.
     *
     * @param message the message that is to be sent.
     * @return the actual message that was sent by quickfixj engine (which incl. session tags etc.).
     */
    Message send(Message message);

    /**
     * Subscribe to a stream of message in the Fix Session, with the specified scope-filter (selector).
     * Remember to close the subscription when the messages received from stream are no longer needed.
     *
     * @param messageSelector the scope filter that will check which messages received are relevant to this subscription.
     * @param onResponse      the callback that will be invoked when a response FIX message is received by quickfixj.
     * @param onError         the callback that will be invoked when an error is received by quickfixj.
     * @return AutoCloseable of the subscription to the fix messages, that match the filter criteria.
     */
    Disposable subscribe(Predicate<Message> messageSelector, Consumer<Message> onResponse,
            Consumer<Throwable> onError);

    /**
     * Convenient method that allows to send a message to the fix session and then subscribe to the response(s) received for this message.
     * The responses are associated with the requests based on the requestId tag in the FIX messages.
     *
     * @param message    the message that is to be sent.
     * @param onResponse the callback that will be invoked when a response FIX message is received by quickfixj.
     * @param onError    the callback that will be invoked when an error is received by quickfixj.
     * @return AutoCloseable of the subscription to the fix messages received by the session associated with the request message, (based on requestId).
     */
    default Disposable sendAndSubscribe(Message message, Consumer<Message> onResponse,
            Consumer<Throwable> onError) {
        return sendAndSubscribe(message, RefIdSelector::new, onResponse, onError);
    }

    /**
     * Convenient method that allows to send a message to the fix session and then subscribe to the response(s) received for this message.
     * The responses are associated with the requests based on the requestId tag in the FIX messages, using the {@link ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.RefIdSelector}.
     *
     * @param message               the message that is to be sent.
     * @param refIdSelectorSupplier a RefIdSelector that will associate a request with a response.
     * @return AutoCloseable of the subscription to the fix messages received by the session associated with the request message.
     */
    Disposable sendAndSubscribe(Message message, Function<Message, RefIdSelector> refIdSelectorSupplier,
            Consumer<Message> onResponse, Consumer<Throwable> onError
    );
}
