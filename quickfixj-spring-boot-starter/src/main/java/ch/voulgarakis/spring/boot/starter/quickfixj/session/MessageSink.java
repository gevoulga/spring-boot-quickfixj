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

package ch.voulgarakis.spring.boot.starter.quickfixj.session;

import quickfix.Message;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MessageSink {

    private final Set<MessageSink> sinks;
    private final Predicate<Message> messageSelector;
    private final Consumer<Message> onNext;
    private final Consumer<Throwable> onError;

    public MessageSink(Set<MessageSink> sinks, Predicate<Message> messageSelector,
            Consumer<Message> onNext, Consumer<Throwable> onError) {
        this.messageSelector = messageSelector;
        this.onNext = onNext;
        this.onError = onError;

        //Add the sink to the registry
        this.sinks = sinks;
        this.sinks.add(this);
    }

    public void dispose() {
        //When sink is disposed (cancelled, terminated) we remove it from the sinks
        sinks.remove(this);
    }

    void next(Message message) {
        onNext.accept(message);
    }

    void error(Throwable error) {
        dispose();
        onError.accept(error);
    }

    public Predicate<Message> getMessageSelector() {
        return messageSelector;
    }
}