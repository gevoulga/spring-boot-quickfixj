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

package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.fix.session.FixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.fix.session.FixSessionImpl;
import org.awaitility.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import quickfix.*;
import quickfix.fix43.QuoteRequest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        properties = {
                "quickfixj.config=classpath:quickfixj.cfg",
                "quickfixj.jmx-enabled=false",
                "quickfixj.startup-timeout="
        })
@DirtiesContext //Stop port already bound issues from other tests
public class QuickfixjApplicationTest {

    private static final AtomicBoolean received = new AtomicBoolean(false);
    @Autowired
    private Application application;

    @Test
    public void onCreateCalled() throws FieldNotFound, IncorrectTagValue, IncorrectDataFormat, UnsupportedMessageType {
        QuoteRequest quoteRequest = new QuoteRequest();
        application.fromApp(quoteRequest, new SessionID("FIX.4.3:TEST_CLIENT->FIX"));
        await().atMost(Duration.FIVE_SECONDS).until(received::get);
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableQuickFixJ
    public static class TestConfig {

        @Bean
        public FixSession fixSession() {
            return new FixSessionImpl() {
                @Override
                protected void received(Message message) {
                    received.set(true);
                }
            };
        }
    }
}