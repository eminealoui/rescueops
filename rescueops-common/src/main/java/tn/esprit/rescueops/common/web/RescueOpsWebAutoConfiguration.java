package tn.esprit.rescueops.common.web;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tn.esprit.rescueops.common.error.GlobalExceptionHandler;
import tn.esprit.rescueops.common.error.ProblemResponses;

/** Filtre de corrélation, gestionnaire d'erreurs commun et propagation Feign. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RescueOpsWebAutoConfiguration {

    /** Avant la sécurité : même un 401 porte l'identifiant de corrélation. */
    @Bean
    FilterRegistrationBean<CorrelationIdFilter> rescueOpsCorrelationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>(new CorrelationIdFilter());
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler rescueOpsGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    /** Appels Feign entre services : propage le token de l'utilisateur et l'identifiant de corrélation. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    static class FeignPropagation {
        @Bean
        feign.RequestInterceptor rescueOpsFeignPropagation() {
            return template -> {
                String id = MDC.get(ProblemResponses.MDC_CORRELATION_ID);
                if (id != null) {
                    template.header(CorrelationIdFilter.HEADER, id);
                }
                if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
                    template.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getToken().getTokenValue());
                }
            };
        }
    }
}
