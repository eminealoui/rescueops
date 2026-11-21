package tn.esprit.rescueops.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class GatewayRoutesTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    void lesSeptServicesEtLeurDocumentationSontRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        Map<String, String> uriById = routes.stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString()));

        assertThat(uriById).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("ms-user", "lb://ms-user"),
                Map.entry("ms-hospital", "lb://ms-hospital"),
                Map.entry("ms-emergency", "lb://ms-emergency"),
                Map.entry("ms-resource", "lb://ms-resource"),
                Map.entry("ms-dispatch", "lb://ms-dispatch"),
                Map.entry("ms-intervention", "lb://ms-intervention"),
                Map.entry("ms-training", "lb://ms-training"),
                Map.entry("docs-ms-user", "lb://ms-user"),
                Map.entry("docs-ms-hospital", "lb://ms-hospital"),
                Map.entry("docs-ms-emergency", "lb://ms-emergency"),
                Map.entry("docs-ms-resource", "lb://ms-resource"),
                Map.entry("docs-ms-dispatch", "lb://ms-dispatch"),
                Map.entry("docs-ms-intervention", "lb://ms-intervention"),
                Map.entry("docs-ms-training", "lb://ms-training")));
    }

    @Test
    void lesPrefixesDuGuideSontConserves() {
        String predicates = routeLocator.getRoutes()
                .filter(r -> r.getId().equals("ms-hospital"))
                .map(r -> r.getPredicate().toString()).blockFirst();

        assertThat(predicates).contains("/api/hospitals/**").contains("/api/transfers/**");
    }
}
