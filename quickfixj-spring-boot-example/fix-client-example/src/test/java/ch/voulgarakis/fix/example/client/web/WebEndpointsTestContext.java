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

package ch.voulgarakis.fix.example.client.web;

import ch.voulgarakis.fix.example.client.FixClientContext;
import ch.voulgarakis.spring.boot.starter.quickfixj.flux.ReactiveFixSession;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.config.EnableWebFlux;
import quickfix.SessionSettings;

@TestConfiguration
@TestPropertySource("classpath:application-web-test.yaml")
public class WebEndpointsTestContext {

    @Bean
    public ReactiveFixSession fixSession() {
        return new SessionMock();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {MetricsAutoConfiguration.class})
    @EnableWebFlux
    @ComponentScan(
            basePackageClasses = FixClientContext.class,
            excludeFilters = {
                    @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FixClientContext.class),
                    @Filter(type = FilterType.REGEX, pattern = ".*TestContext")
            }
    )
    public static class WebEndpointsSpringTestContext {
        @Bean
        public SessionSettings sessionSettings() {
            return new SessionSettings();
        }
    }
}
