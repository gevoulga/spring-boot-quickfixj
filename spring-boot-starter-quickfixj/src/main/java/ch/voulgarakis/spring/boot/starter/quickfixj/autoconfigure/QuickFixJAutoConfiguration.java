package ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure;

import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.connection.FixConnection;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings;
import org.quickfixj.jmx.JmxExporter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;

import javax.management.JMException;
import javax.management.ObjectName;
import java.util.List;
import java.util.Optional;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings.createConnector;

@Configuration
@AutoConfigurationPackage
@EnableConfigurationProperties(QuickFixJBootProperties.class)
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
//@ConditionalOnBean(Application.class)
//@Conditional(QuickFixJAutoConfigurationConditional.class)
//@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class QuickFixJAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SessionSettings sessionSettings(QuickFixJBootProperties properties) {
        return FixSessionSettings.loadSettings(properties.getConfig());
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageStoreFactory messageStoreFactory() {
        return new MemoryStoreFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public Connector connector(Application application, SessionSettings sessionSettings, MessageStoreFactory messageStoreFactory, MessageFactory messageFactory, Optional<LogFactory> logFactory) {
        try {
            return createConnector(application, messageStoreFactory, sessionSettings, logFactory.orElse(null), messageFactory);
        } catch (ConfigError | FieldConvertError configError) {
            throw new QuickFixJConfigurationException(configError.getMessage(), configError);
        }
    }

    @Bean
    public FixConnection fixConnection(Connector connector) {
        return new FixConnection(connector);
    }

    @Bean
    @ConditionalOnMissingBean
    public Application application(SessionSettings sessionSettings, List<FixSession> sessions) {
        if (sessionSettings.size() != 0 && (sessions == null || sessions.isEmpty())) {
            throw new QuickFixJConfigurationException(String.format(
                    "You need to define %s beans that will handle FIX message exchange.",
                    FixSession.class.getSimpleName()));
        }
        return new FixSessionManager(sessionSettings, sessions);
    }

    @Bean
    @ConditionalOnProperty(prefix = "quickfixj", name = "jmx-enabled", havingValue = "true")
    @ConditionalOnClass(JmxExporter.class)
    @ConditionalOnSingleCandidate(Connector.class)
    @ConditionalOnMissingBean
    public ObjectName connectorMBean(Connector connector) {
        try {
            JmxExporter exporter = new JmxExporter();
            return exporter.register(connector);
        } catch (JMException e) {
            throw new QuickFixJConfigurationException(e.getMessage(), e);
        }
    }
}