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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import quickfix.SessionID;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.fix43.QuoteRequest;
import reactor.core.publisher.Flux;

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
    private ReactiveFixSession fixSession;

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
}
