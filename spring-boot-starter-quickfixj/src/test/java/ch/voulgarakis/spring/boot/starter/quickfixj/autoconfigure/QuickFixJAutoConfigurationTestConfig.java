package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.RejectException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSession;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.Message;
import quickfix.RejectLogon;

@Configuration
@EnableAutoConfiguration
@EnableQuickFixJ
public class QuickFixJAutoConfigurationTestConfig {

    @Bean
    public FixSession fixSession() {
        return new FixSession() {
            @Override
            protected void received(Message message) {
            }

            @Override
            protected void error(RejectException message) {
            }

            @Override
            protected void authenticate(Message message) throws RejectLogon {
            }
        };
    }
}
