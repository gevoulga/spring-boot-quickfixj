package ch.voulgarakis.spring.boot.actuator.quickfixj.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.invoke.convert.ConversionServiceParameterValueMapper;
import org.springframework.boot.actuate.endpoint.invoker.cache.CachingOperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.DefaultConversionService;

public class WebDiscoverer {

    public static Map<EndpointId, ExposableWebEndpoint> findWebEndpoints(ApplicationContext context) {
        ConversionServiceParameterValueMapper parameterMapper = new ConversionServiceParameterValueMapper(
            DefaultConversionService.getSharedInstance());
        EndpointMediaTypes mediaTypes = new EndpointMediaTypes(
            Collections.singletonList("application/json"),
            Collections.singletonList("application/json"));

        WebEndpointDiscoverer discoverer = new WebEndpointDiscoverer(context,
            parameterMapper,
            mediaTypes,
            Collections.singletonList(EndpointId::toString),
            Collections.singleton(new CachingOperationInvokerAdvisor((id) -> null)),
            Collections.emptyList());

        return mapEndpoints(discoverer.getEndpoints());
    }

    private static Map<EndpointId, ExposableWebEndpoint> mapEndpoints(Collection<ExposableWebEndpoint> endpoints) {
        Map<EndpointId, ExposableWebEndpoint> endpointById = new HashMap<>();
        endpoints.forEach((endpoint) -> endpointById.put(endpoint.getEndpointId(), endpoint));
        return endpointById;
    }
}
