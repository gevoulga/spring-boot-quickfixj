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
import ch.voulgarakis.spring.boot.starter.quickfixj.connection.FixConnection;
import ch.voulgarakis.spring.boot.starter.quickfixj.exception.QuickFixJConfigurationException;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.*;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingId;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.quickfixj.jmx.JmxExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import quickfix.*;

import javax.management.JMException;
import javax.management.ObjectName;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionUtils.extractSessionNames;
import static java.lang.String.format;

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
    public SessionSettings sessionSettings(QuickFixJBootProperties properties, Environment environment,
                                           ResourceLoader resourceLoader) {
        return FixSessionSettings.loadSettings(properties.getConfig(), environment, resourceLoader);
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
    public FixConnectionType fixConnectionType(SessionSettings sessionSettings) throws FieldConvertError, ConfigError {
        return FixConnectionType.of(sessionSettings);
    }


    @Bean
    @ConditionalOnMissingBean
    public Connector connector(Application application, FixConnectionType fixConnectionType,
                               SessionSettings sessionSettings, MessageStoreFactory messageStoreFactory,
                               MessageFactory messageFactory, Optional<LogFactory> logFactory) {
        try {
            return FixSessionSettings
                    .createConnector(application, fixConnectionType, messageStoreFactory, sessionSettings,
                            logFactory.orElse(null), messageFactory);
        } catch (ConfigError configError) {
            throw new QuickFixJConfigurationException(configError.getMessage(), configError);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public StartupLatch startupLatch(SessionSettings sessionSettings,
                                     @Value("${quickfixj.startup.timeout}") Duration timeout)
            throws FieldConvertError, ConfigError {
        return new StartupLatch(sessionSettings.size(), FixConnectionType.of(sessionSettings), timeout);
    }

    @Bean
    @ConditionalOnMissingBean
    public FixConnection fixConnection(Connector connector, StartupLatch startupLatch) {
        return new FixConnection(connector, startupLatch);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingId loggingId() {
        return new LoggingId();
    }

    @Bean
    @ConditionalOnMissingBean
    public Application application(SessionSettings sessionSettings, FixConnectionType fixConnectionType,
                                   List<AbstractFixSession> sessions, StartupLatch startupLatch, LoggingId loggingId) {
        if (sessionSettings.size() != 0 && (sessions == null || sessions.isEmpty())) {
            throw new QuickFixJConfigurationException(format(
                    "You need to define %s beans that will handle FIX message exchange for the sessions: %s",
                    FixSession.class.getSimpleName(), extractSessionNames(sessionSettings)));
        }
        return new FixSessionManager(sessionSettings, fixConnectionType, sessions, startupLatch, loggingId);
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