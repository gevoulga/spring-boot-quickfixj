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

package ch.voulgarakis.spring.boot.actuator.quickfixj.config;

import ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint.QuickFixJEndpoint;
import ch.voulgarakis.spring.boot.actuator.quickfixj.health.QuickFixJHealthIndicator;
import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.FixSessionInterface;
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJConnectionAutoConfiguration;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
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
@AutoConfigureAfter(QuickFixJConnectionAutoConfiguration.class)
@ConditionalOnClass({Connector.class, SessionSettings.class})
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
public class QuickFixJEndpointAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnAvailableEndpoint(endpoint = QuickFixJEndpoint.class)
    public QuickFixJEndpoint quickfixjEndpoint(SessionSettings sessionSettings,
            InternalFixSessions<? extends FixSessionInterface> fixSessions) {
        return new QuickFixJEndpoint(sessionSettings, fixSessions);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledHealthIndicator("quickfixj")
    public QuickFixJHealthIndicator quickfixjHealthIndicator(Connector connector) {
        return new QuickFixJHealthIndicator(connector);
    }
}