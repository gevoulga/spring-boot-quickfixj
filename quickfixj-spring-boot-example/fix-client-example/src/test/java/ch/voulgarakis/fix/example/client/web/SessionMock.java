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

package ch.voulgarakis.fix.example.client.web;

import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSessionMock;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import quickfix.Message;
import quickfix.field.*;
import quickfix.fix43.ExecutionReport;
import quickfix.fix43.Quote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils.isMessageOfType;

public class SessionMock extends ReactiveFixSessionMock {

    @Override
    protected Flux<Message> messageGenerator(String quoteReqId, Message request) {
        //Quote request
        if (isMessageOfType(request, MsgType.QUOTE_REQUEST)) {
            return quotes(request);
        }
        //Booking request
        else if (isMessageOfType(request, MsgType.ORDER_SINGLE)) {
            return execution(request);
        }
        //Not valid - do not return anything
        else {
            return null;
        }
    }

    private Flux<Message> quotes(Message quoteRequest) {
        return Flux
                .interval(Duration.ofSeconds(1))
                .map(tick -> {
                    Quote quote = new Quote(new QuoteID(Long.toString(tick)));
                    String requestId = FixMessageUtils.safeGetField(quoteRequest, new QuoteReqID()).orElse(null);
                    quote.set(new QuoteReqID(requestId));
                    return quote;
                });
    }

    private Flux<Message> execution(Message newOrderSingle) {
        return Mono
                .delay(Duration.ofSeconds(1))
                .flatMapMany(tick -> {
                    ExecutionReport executionReport = new ExecutionReport();
                    String clrOrdId = FixMessageUtils.safeGetField(newOrderSingle, new ClOrdID()).orElse(null);
                    executionReport.set(new ClOrdID(clrOrdId));
                    executionReport.set(new ExecID(("Execution-" + tick)));
                    executionReport.set(new OrdStatus(OrdStatus.FILLED));
                    return Flux.just(executionReport);
                });
    }
}
