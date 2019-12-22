package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.SessionException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.Message;

@Configuration
@EnableAutoConfiguration
@EnableQuickFixJ
public class QuickFixJAutoConfigurationTestConfig {

    @Bean
    public AbstractFixSession fixSession() {
        return new AbstractFixSession() {
            @Override
            protected void received(Message message) {
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
