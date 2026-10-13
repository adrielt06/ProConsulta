package com.proconsulta.backend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class TenantFilterTests {

	private final TenantFilter filter = new TenantFilter();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void exposesTenantFromJwtDuringRequestAndClearsItAfter() throws Exception {
		authenticateWith(Map.of("sub", "1", TenantFilter.TENANT_CLAIM, 7L));
		AtomicReference<Long> seen = new AtomicReference<>();

		run((request, response) -> seen.set(TenantContext.require()));

		assertThat(seen.get()).isEqualTo(7L);
		assertThat(TenantContext.currentOrNull()).isNull();
	}

	@Test
	void clearsTenantWhenRequestFails() {
		authenticateWith(Map.of("sub", "1", TenantFilter.TENANT_CLAIM, 7L));

		assertThatThrownBy(() -> run((request, response) -> {
			throw new ServletException("falla el endpoint");
		})).isInstanceOf(ServletException.class);

		assertThat(TenantContext.currentOrNull()).isNull();
	}

	@Test
	void withoutTenantClaimThereIsNoTenant() throws Exception {
		authenticateWith(Map.of("sub", "1"));

		run((request, response) -> assertThatThrownBy(TenantContext::require)
			.isInstanceOf(IllegalStateException.class));
	}

	@Test
	void resolverFallsBackToNoTenant() {
		TenantIdentifierResolver resolver = new TenantIdentifierResolver();

		assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(TenantIdentifierResolver.NO_TENANT);
		TenantContext.set(7L);
		try {
			assertThat(resolver.resolveCurrentTenantIdentifier()).isEqualTo(7L);
		}
		finally {
			TenantContext.clear();
		}
	}

	private void run(FilterChain chain) throws Exception {
		this.filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
	}

	private static void authenticateWith(Map<String, Object> claims) {
		Instant now = Instant.now();
		Jwt jwt = new Jwt("token", now, now.plusSeconds(60), Map.of("alg", "HS256"), claims);
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

}
