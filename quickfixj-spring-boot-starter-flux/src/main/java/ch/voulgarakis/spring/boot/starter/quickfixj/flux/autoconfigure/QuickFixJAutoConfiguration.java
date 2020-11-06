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

package ch.voulgarakis.spring.boot.starter.quickfixj.flux.autoconfigure;


import ch.voulgarakis.spring.boot.starter.quickfixj.EnableQuickFixJ;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveAbstractFixSession;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSessions;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.logging.ReactiveMdcContextConfiguration;
import ch.voulgarakis.spring.boot.starter.quickfixj.session.InternalFixSessions;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.SessionSettings;

import java.util.List;

@Configuration
@AutoConfigurationPackage
@ConditionalOnBean(annotation = EnableQuickFixJ.class)
@AutoConfigureBefore(ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfiguration.class)
//@ConditionalOnBean(Application.class)
//@Conditional(QuickFixJAutoConfigurationConditional.class)
//@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class QuickFixJAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(InternalFixSessions.class)
    public ReactiveFixSessions fixSessions(SessionSettings sessionSettings,
            List<ReactiveAbstractFixSession> sessions) {
        return new ReactiveFixSessions(sessionSettings, sessions);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "logging.rxMDC", havingValue = "true")
    public ReactiveMdcContextConfiguration reactiveMdcContextConfiguration() {
        return new ReactiveMdcContextConfiguration();
    }
}