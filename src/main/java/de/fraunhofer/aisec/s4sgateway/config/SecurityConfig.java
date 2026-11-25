package de.fraunhofer.aisec.s4sgateway.config;

import de.fraunhofer.aisec.s4sgateway.security.GatewayAccessProperties;
import de.fraunhofer.aisec.s4sgateway.security.PartnerIdentityService;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static de.fraunhofer.aisec.s4sgateway.security.GatewayAccessProperties.Permission.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayAccessProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain gatewaySecurity(HttpSecurity http, PartnerIdentityService identities,
                                        GatewayAccessProperties properties,
                                        HandlerMappingIntrospector introspector) throws Exception {
        var networks = properties.internalNetworks().stream().map(IpAddressMatcher::new).toList();
        AuthorizationManager<RequestAuthorizationContext> internalNetwork = (authentication, context) ->
                new AuthorizationDecision(networks.stream().anyMatch(network -> network.matches(context.getRequest())));
        RequestMatcher read = request -> HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod());

        // The API accepts cross-system clients and same-origin clients. Cross-origin browser requests are denied.
        var cors = new UrlBasedCorsConfigurationSource();
        cors.registerCorsConfiguration("/**", new CorsConfiguration());
        var mvc = new MvcRequestMatcher.Builder(introspector);
        RequestMatcher adminWrite = new OrRequestMatcher(
                mvc.pattern(HttpMethod.POST, "/setisa"),
                mvc.pattern(HttpMethod.POST, "/reloadconfig"));

        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .cors(config -> config.configurationSource(cors))
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(adminWrite)
                        .csrfTokenRepository(new CookieCsrfTokenRepository()))
                .x509(x509 -> x509.x509PrincipalExtractor(PartnerIdentityService::fingerprint)
                        .userDetailsService(identities))
                .authorizeHttpRequests(requests -> requests
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(new AndRequestMatcher(EndpointRequest.to("health"), read))
                        .access(internalNetwork)
                        .requestMatchers(new AndRequestMatcher(EndpointRequest.to("metrics"), read))
                        .access(AuthorizationManagers.allOf(internalNetwork,
                                AuthorityAuthorizationManager.hasAuthority(MONITORING_SERVICE.name())))
                        .requestMatchers(EndpointRequest.toAnyEndpoint(), EndpointRequest.toLinks()).denyAll()
                        .requestMatchers(HttpMethod.GET, "/status", "/isa").hasAuthority(ISA_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/setisa", "/isacheck").hasAuthority(ISA_ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/reloadconfig").hasAuthority(GATEWAY_ADMIN.name())
                        .requestMatchers(HttpMethod.GET, "/csrf").hasAnyAuthority(ISA_ADMIN.name(), GATEWAY_ADMIN.name())
                        .requestMatchers("/status", "/isa", "/setisa", "/isacheck", "/reloadconfig", "/csrf",
                                "/status/**", "/isa/**", "/setisa/**", "/isacheck/**", "/reloadconfig/**", "/csrf/**",
                                "/actuator", "/actuator/**", "/error").denyAll()
                        .requestMatchers(HttpMethod.GET, "/**").hasAuthority(SOC.name())
                        .requestMatchers(HttpMethod.POST, "/**").hasAuthority(SOC.name())
                        .anyRequest().denyAll())
                .build();
    }
}
