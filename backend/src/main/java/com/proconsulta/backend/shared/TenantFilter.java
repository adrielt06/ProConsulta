package com.proconsulta.backend.shared;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// No es un @Component a propósito: Spring Boot lo registraría también como filtro del
// servlet, antes de la seguridad, cuando todavía no hay JWT. Lo agrega SecurityConfig.
class TenantFilter extends OncePerRequestFilter {

	static final String TENANT_CLAIM = "tenant_id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwt
				&& jwt.getToken().getClaim(TENANT_CLAIM) instanceof Number tenantId) {
			TenantContext.set(tenantId.longValue());
		}
		try {
			chain.doFilter(request, response);
		}
		finally {
			// También cuando el request falla: si no, el próximo request que reuse este
			// hilo hereda el consultorio y devuelve datos ajenos sin dar error.
			TenantContext.clear();
		}
	}

}
