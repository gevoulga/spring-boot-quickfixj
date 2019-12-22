package ch.voulgarakis.spring.boot.actuator.quickfixj.config;

import ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint.QuickFixJEndpoint;
import ch.voulgarakis.spring.boot.actuator.quickfixj.health.QuickFixJHealthIndicator;
import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.Connector;
import quickfix.SessionSettings;

@Configuration
@AutoConfigureAfter(QuickFixJAutoConfiguration.class)
@ConditionalOnClass({Connector.class, SessionSettings.class})
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
public class QuickFixJEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledEndpoint(endpoint = QuickFixJEndpoint.class)
    public QuickFixJEndpoint quickFixJEndpoint(SessionSettings sessionSettings) {
        return new QuickFixJEndpoint(sessionSettings);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("quickFixJ")
    public QuickFixJHealthIndicator quickFixJHealthIndicator(Connector connector) {
        return new QuickFixJHealthIndicator(connector);
    }
}