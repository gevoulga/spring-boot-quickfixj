package ch.voulgarakis.spring.boot.actuator.quickfixj.config;

import ch.voulgarakis.spring.boot.actuator.quickfixj.endpoint.QuickFixJEndpoint;
import ch.voulgarakis.spring.boot.starter.quickfixj.autoconfigure.QuickFixJAutoConfigurationTestConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static ch.voulgarakis.spring.boot.actuator.quickfixj.util.WebDiscoverer.findWebEndpoints;
import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
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
        assertThat(quickFixJEndpoint).isNull();
    }

    @Test
    public void testAutoConfiguredEndpoints() {
        Map<EndpointId, ExposableWebEndpoint> endpoints = findWebEndpoints(context);
        assertThat(endpoints).doesNotContainKey(ENDPOINT_ID);
    }
}