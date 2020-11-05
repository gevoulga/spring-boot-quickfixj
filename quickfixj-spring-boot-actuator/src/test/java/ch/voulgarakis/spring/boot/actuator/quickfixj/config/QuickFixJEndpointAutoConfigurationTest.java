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
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfigurationTestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.InvocationContext;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.invoke.reflect.ReflectiveOperationInvoker;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static ch.voulgarakis.spring.boot.actuator.quickfixj.util.WebDiscoverer.findWebEndpoints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = QuickFixJAutoConfigurationTestConfig.class)
@TestPropertySource("classpath:application.properties")
public class QuickFixJEndpointAutoConfigurationTest {

    private static final EndpointId ENDPOINT_ID = EndpointId.of("quickfixj");

    @Autowired
    private QuickFixJEndpoint quickFixJEndpoint;
    @Autowired
    private ApplicationContext context;

    @Test
    public void testAutoConfiguredBeans() {
        assertNotNull(quickFixJEndpoint);
        assertEquals(1, quickFixJEndpoint.readProperties().size());
    }

    @Test
    public void testAutoConfiguredEndpoints() {
        Map<EndpointId, ExposableWebEndpoint> endpoints = findWebEndpoints(context);
        assertThat(endpoints).containsOnlyKeys(ENDPOINT_ID);
    }

    @Test
    public void shouldReadProperties() {
        Map<EndpointId, ExposableWebEndpoint> endpoints = findWebEndpoints(context);
        assertThat(endpoints).containsKey(ENDPOINT_ID);

        ExposableWebEndpoint endpoint = endpoints.get(ENDPOINT_ID);
        assertEquals(1, endpoint.getOperations().size());

        WebOperation operation = endpoint.getOperations().iterator().next();
        Object invoker = ReflectionTestUtils.getField(operation, "invoker");
        assertThat(invoker).isInstanceOf(ReflectiveOperationInvoker.class);

        Map<String, Properties> properties =
                (Map<String, Properties>) ((ReflectiveOperationInvoker) invoker).invoke(
                        new InvocationContext(mock(SecurityContext.class), Collections.emptyMap()));
        assertEquals(1, properties.size());
    }
}