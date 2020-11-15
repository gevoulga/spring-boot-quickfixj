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

import ch.voulgarakis.spring.boot.actuator.quickfixj.QuickFixJAutoConfigurationTestConfig;
import ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint.QuickFixJEndpoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static ch.voulgarakis.spring.boot.actuator.quickfixj.util.WebDiscoverer.findWebEndpoints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = QuickFixJAutoConfigurationTestConfig.class)
@TestPropertySource("classpath:application-noendpoint.properties")
public class QuickFixJNoEndpointAutoConfigurationTest {

    private static final EndpointId ENDPOINT_ID = EndpointId.of("quickfixj");

    @Autowired(required = false)
    private QuickFixJEndpoint quickFixJEndpoint;
    @Autowired
    private ApplicationContext context;

    @Test
    public void testAutoConfiguredBeans() {
        assertNull(quickFixJEndpoint);
    }

    @Test
    public void testAutoConfiguredEndpoints() {
        Map<EndpointId, ExposableWebEndpoint> endpoints = findWebEndpoints(context);
        assertThat(endpoints).doesNotContainKey(ENDPOINT_ID);
    }
}