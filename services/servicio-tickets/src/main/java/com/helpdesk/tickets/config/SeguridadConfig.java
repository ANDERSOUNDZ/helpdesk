package com.helpdesk.tickets.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private static final String EMISOR = "helpdesk-identidad";
    private static final int LARGO_MINIMO_SECRETO = 32;

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http,
                                                 @Value("${app.auth.enabled:false}") boolean activa,
                                                 @Value("${app.jwt.secret:}") String secreto) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!activa) {
            http.authorizeHttpRequests(reglas -> reglas.anyRequest().permitAll());
            return http.build();
        }

        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException("AUTH_ENABLED=true exige JWT_SECRET de al menos "
                    + LARGO_MINIMO_SECRETO + " caracteres");
        }

        http.authorizeHttpRequests(reglas -> reglas
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/tickets/*/asignar/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/tickets/*/resolver").hasAnyRole("TECNICO", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/tickets/*/reabrir").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/tickets/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/tickets/*").hasRole("ADMINISTRADOR")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(recurso -> recurso.jwt(jwt -> jwt
                        .decoder(decodificador(secreto))
                        .jwtAuthenticationConverter(convertidor())));
        return http.build();
    }

    private JwtDecoder decodificador(String secreto) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISOR));
        return decodificador;
    }

    private JwtAuthenticationConverter convertidor() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }
}
