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


import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.Message;
import quickfix.SessionID;
import quickfix.field.QuoteID;
import quickfix.field.QuoteReqID;
import quickfix.fix43.Quote;
import quickfix.fix43.QuoteRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ReactiveFixSessionTestContext.class)
@TestPropertySource("classpath:fixSessionTest.properties")
@DirtiesContext //Stop port already bound issues from other tests
public class ReactiveFixSessionTest {
    private static final Logger LOG = LoggerFactory.getLogger(ReactiveFixSessionTest.class);

    private static final SessionID SESSION_ID = new SessionID("FIX.4.3", "TEST_CLIENT", "FIX");
    @Autowired
    private FixSessionManager sessionManager;
    @Autowired
    private ReactiveFixSessionImpl fixSession;

    @Test
    public void testBurstSubscription() {
        QuoteRequest quoteRequest = new QuoteRequest(new QuoteReqID(Long.toString(0L)));
        Flux<Message> messageFlux = fixSession.sendAndSubscribe(() -> quoteRequest)
                .doOnSubscribe(subscription -> burstOf20QuotesWithDifferentIdEvery100millis());

        StepVerifier
                .create(messageFlux)
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(100))
                .expectNextCount(20)
                .thenAwait(Duration.ofMillis(200))
                .expectNextCount(20)
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    private void burstOf20QuotesWithDifferentIdEvery100millis() {
        Flux.interval(Duration.ofMillis(100))
                .take(2)
                .repeat(1)
                .flatMap(i -> {
                    LOG.debug("tick: {}", i);
                    Quote quote = new Quote(new QuoteID(Long.toString(i)));
                    quote.set(new QuoteReqID(Long.toString(i)));
                    return Flux.just(quote)
                            .repeat(19)
                            .map(Message::clone);
                })
                .parallel()
                .runOn(Schedulers.elastic())
                .subscribe(quote -> {
                    LOG.debug("Sending to Session Manager: {}", quote);
                    sessionManager.fromApp((Message) quote, SESSION_ID);
                });
        LOG.info("Sending burst of quotes");
    }

    @Test
    public void testRandomSubscription() {
        List<Long> longs = new Random()
                .longs(1000, 0, 5).boxed()
                .collect(Collectors.toList());
        long count = longs.stream().filter(i -> i == 0).count();
        LOG.info("Random quotes: {}", count);

        AtomicInteger counter = new AtomicInteger();
        QuoteRequest quoteRequest = new QuoteRequest(new QuoteReqID(Long.toString(0L)));
        Flux<Message> messageFlux = fixSession.sendAndSubscribe(() -> quoteRequest)
                .doOnSubscribe(subscription -> randomQuotes(longs))
                .doOnNext(message -> LOG.info("Progress: {}/{}", counter.incrementAndGet(), count));

        StepVerifier
                .create(messageFlux)
                .expectSubscription()
                .expectNextCount(count)
                .thenCancel()
                .verify(Duration.ofSeconds(10));
    }


    private void randomQuotes(List<Long> longs) {
        Mono.delay(Duration.ofMillis(100))
                .thenMany(Flux
                        .fromIterable(longs)
                        .map(i -> {
                            LOG.debug("tick: {}", i);
                            Quote quote = new Quote(new QuoteID(Long.toString(i)));
                            quote.set(new QuoteReqID(Long.toString(i)));
                            return quote;
                        })
                        .parallel()
                        .runOn(Schedulers.elastic())
                )
                .subscribe(quote -> {
                    LOG.debug("Sending to Session Manager: {}", quote);
                    sessionManager.fromApp(quote, SESSION_ID);
                });
        LOG.info("Sending random quotes");
    }
}