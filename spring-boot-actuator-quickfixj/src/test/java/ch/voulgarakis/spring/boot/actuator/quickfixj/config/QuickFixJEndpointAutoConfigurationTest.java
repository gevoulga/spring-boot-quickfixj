package ch.voulgarakis.spring.boot.actuator.quickfixj.config;

import ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint.QuickFixJEndpoint;
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfigurationTestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import static ch.voulgarakis.spring.boot.actuator.quickfixj.util.WebDiscoverer.findWebEndpoints;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
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
        assertThat(quickFixJEndpoint).isNotNull();
        assertThat(quickFixJEndpoint.readProperties().size()).isEqualTo(1);
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
        assertThat(endpoint.getOperations()).hasSize(1);

        WebOperation operation = endpoint.getOperations().iterator().next();
        Object invoker = ReflectionTestUtils.getField(operation, "invoker");
        assertThat(invoker).isInstanceOf(ReflectiveOperationInvoker.class);

        Map<String, Properties> properties =
                (Map<String, Properties>) ((ReflectiveOperationInvoker) invoker).invoke(
                        new InvocationContext(mock(SecurityContext.class), Collections.emptyMap()));
        assertThat(properties).hasSize(1);
    }
}