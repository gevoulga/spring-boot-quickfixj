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
import ch.voulgarakis.spring.boot.starter.quickfixj.authentication.SessionSettingsAuthenticationService;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixConnectionType;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.FixSessionSettings;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.logging.LoggingId;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.utils.StartupLatch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import quickfix.*;

import java.time.Duration;

@Configuration
@AutoConfigurationPackage
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
@EnableConfigurationProperties(QuickFixJBootProperties.class)
public class QuickFixJSessionSettingsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SessionSettings sessionSettings(QuickFixJBootProperties properties, Environment environment,
            ResourceLoader resourceLoader) {
        return FixSessionSettings.loadSettings(properties.getConfig(), environment, resourceLoader);
    }

    @Bean
    @ConditionalOnMissingBean
    public FixConnectionType fixConnectionType(SessionSettings sessionSettings) {
        return FixConnectionType.of(sessionSettings);
    }

    @Bean
    @ConditionalOnMissingBean
    public StartupLatch startupLatch(SessionSettings sessionSettings,
            @Value("${quickfixj.startup.timeout}") Duration timeout) {
        return new StartupLatch(sessionSettings.size(), FixConnectionType.of(sessionSettings), timeout);
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggingId loggingId() {
        return new LoggingId();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationService authenticationService(SessionSettings sessionSettings) {
        return new SessionSettingsAuthenticationService(sessionSettings);
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
}
