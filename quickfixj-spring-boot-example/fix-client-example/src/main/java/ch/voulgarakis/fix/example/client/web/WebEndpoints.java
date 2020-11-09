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

import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSessions;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging.LoggingUtils;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingContext;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.FixMessageUtils;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StaticExtractor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import quickfix.SessionID;
import quickfix.field.*;
import quickfix.fix43.NewOrderSingle;
import quickfix.fix43.QuoteRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

//@DependsOn("reactiveFixSessions")
@RestController
@RequestMapping("/fix")
public class WebEndpoints {

    //We can either refer the session name directly
    @Autowired
    //The session bean is named TEST (in quickfixj.cfg -> SessionName)
    //If we had more than one sessions defined, then the Qualifier would be needed
    //(or naming the session field appropriately - spring bean naming)
    //@Qualifier("TEST")
    private final ReactiveFixSession fixSession;

    @Autowired
    public WebEndpoints(ReactiveFixSession fixSession) {
//        reactiveFixSessions.getFixSessions();
        this.fixSession = fixSession;
    }

    /**
     * Or we could wire-in the {@link ReactiveFixSessions} which is registry of all the sessions.
     * Then, we could get the {@link ReactiveFixSession} by calling any of:
     * {@link ReactiveFixSessions#get()}
     * {@link ReactiveFixSessions#get(SessionID)}
     * {@link ReactiveFixSessions#get(String)}
     */
    //private final ReactiveFixSessions reactiveFixSessions;
    @GetMapping("quote")
    @ResponseStatus(HttpStatus.OK)
    public Flux<String> quotesFromFixServer() {

        //Generate a random UUID
        String id = UUID.randomUUID().toString();

        //Set the logging context
        try (LoggingContext ignored = LoggingUtils.loggingContext(id)) {

            return fixSession
                    //Send the fix message (in the supplier) and subscribe at the responses
                    .sendAndSubscribe(() -> {
                        //Create a mock quote request and set the request id!
                        return new QuoteRequest(new QuoteReqID(id));
                    })
                    //take the first response
                    .take(1)
                    //Get the quoteId
                    .map(quote -> FixMessageUtils
                            .safeGetField(quote, new QuoteID())
                            .orElse("not-found"))

                    //Set the logging context in the reactive processing chain
                    .subscriberContext(LoggingUtils.withLoggingContext())
                    //Export metrics
                    .metrics();
        }
    }


    @RequestMapping(path = "book", method = {RequestMethod.PUT, RequestMethod.POST, RequestMethod.PATCH})
    public Mono<String> bookQuoteFromFixServer(@RequestBody String quoteId) {
        if (StringUtils.isBlank(quoteId)) {
            return Mono.error(new IllegalArgumentException("No quoteId defined"));
        }

        //Set the logging context
        try (LoggingContext ignored = LoggingUtils.loggingContext(quoteId)) {

            return fixSession
                    //Send the fix message (in the supplier) and subscribe at the responses
                    .sendAndSubscribe(() -> {
                        //Create a mock new order request and set the quote id!
                        NewOrderSingle newOrderSingle = new NewOrderSingle();
                        newOrderSingle.set(new ClOrdID(quoteId));
                        return newOrderSingle;
                    })
                    //take the first response
                    .next()
                    //Get the quoteId
                    .map(executionReport -> {
                        //The execution Id
                        String executionId = FixMessageUtils
                                .safeGetField(executionReport, new ExecID())
                                .orElse("not-found");

                        //The Order status
                        String ordStatus = FixMessageUtils
                                .safeGetField(executionReport, new OrdStatus())
                                //Convert the character code of the OrdStatus into text
                                .map(c -> StaticExtractor.toText(new OrdStatus(), c))
                                .orElse("not-found");

                        return executionId + "/" + ordStatus;
                    })

                    //Set the logging context in the reactive processing chain
                    .subscriberContext(LoggingUtils.withLoggingContext())
                    //Export metrics
                    .metrics();
        }
    }
}
