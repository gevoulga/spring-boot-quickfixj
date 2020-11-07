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
import ch.voulgarakis.spring.boot.starter.quickfixj.authentication.AuthenticationService;
import ch.voulgarakis.spring.boot.starter.quickfixj.connection.FixConnection;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.AbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionManager;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingId;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.quickfixj.jmx.JmxExporter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;

import javax.management.JMException;
import javax.management.ObjectName;
import java.util.Optional;

@Configuration
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
//@ConditionalOnBean(Application.class)
//@Conditional(QuickFixJAutoConfigurationConditional.class)
//@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class QuickFixJConnectionAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    public Connector connector(Application application, FixConnectionType fixConnectionType,
            SessionSettings sessionSettings, MessageStoreFactory messageStoreFactory,
            MessageFactory messageFactory, Optional<LogFactory> logFactory) throws ConfigError {
        return fixConnectionType
                .createConnector(application, messageStoreFactory, sessionSettings, logFactory.orElse(null),
                        messageFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public FixConnection fixConnection(Connector connector, StartupLatch startupLatch) {
        return new FixConnection(connector, startupLatch);
    }

    @Bean
    @ConditionalOnMissingBean
    public Application application(InternalFixSessions<? extends AbstractFixSession> fixSessions,
            FixConnectionType fixConnectionType, StartupLatch startupLatch, LoggingId loggingId,
            AuthenticationService authenticationService) {
        return new FixSessionManager(fixSessions, fixConnectionType, startupLatch, loggingId, authenticationService);
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