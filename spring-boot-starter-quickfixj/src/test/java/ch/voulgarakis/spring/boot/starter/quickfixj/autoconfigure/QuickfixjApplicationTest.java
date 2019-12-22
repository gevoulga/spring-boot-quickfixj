package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import org.awaitility.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import quickfix.*;
import quickfix.fix43.QuoteRequest;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(
        properties = {
                "quickfixj.config=classpath:quickfixj.cfg",
                "quickfixj.jmx-enabled=false"
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
        public AbstractFixSession fixSession() {
            return new AbstractFixSession() {
                @Override
                protected void received(Message message) {
                    received.set(true);
                }

                @Override
                protected void error(SessionException message) {
                }

                @Override
                protected void authenticate(Message message) {
                }
            };
        }
    }
}