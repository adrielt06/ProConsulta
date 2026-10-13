package com.proconsulta.backend.shared;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;

@Configuration
class SecurityConfig {

	// Rutas sin token (endpoints-mvp.md). Tienen cadena propia, sin validación de JWT:
	// en la cadena privada un Bearer vencido responde 401 aunque la ruta sea permitAll,
	// y eso rompería justo a /api/auth/refresh.
	@Bean
	@Order(1)
	SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
		return stateless(http)
			.securityMatcher("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/public/**")
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain privateChain(HttpSecurity http) throws Exception {
		return stateless(http)
			.authorizeHttpRequests(auth -> auth
				// El reenvío interno a /error no es un request del cliente; sin esto
				// un 404 de una ruta pública saldría como 401.
				.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
				.anyRequest().authenticated())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			// Después de validar el JWT, que es de donde sale el consultorio.
			.addFilterAfter(new TenantFilter(), BearerTokenAuthenticationFilter.class)
			.build();
	}

	// Sin sesión en el servidor: cada request se autentica con su token. Sin CSRF porque
	// la credencial viaja en el header Authorization, que el navegador no adjunta solo.
	private static HttpSecurity stateless(HttpSecurity http) throws Exception {
		return http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
