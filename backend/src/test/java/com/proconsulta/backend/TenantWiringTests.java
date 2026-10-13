package com.proconsulta.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.proconsulta.backend.shared.TenantContext;

import jakarta.persistence.EntityManagerFactory;

// Comprueba el cableado completo: JWT real -> filtro -> contexto -> Hibernate.
// El aislamiento entre dos consultorios se prueba en el paso 2, con la primera entidad.
@Import({ TestcontainersConfiguration.class, TenantWiringTests.ProbeController.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class TenantWiringTests {

	@LocalServerPort
	int port;

	@Autowired
	JwtEncoder jwtEncoder;

	@Autowired
	EntityManagerFactory entityManagerFactory;

	@Test
	void tenantClaimReachesTheEndpoint() throws Exception {
		assertThat(get(tokenFor(7L)).body()).isEqualTo("7");
		// Otro consultorio en el request siguiente: no queda nada del anterior.
		assertThat(get(tokenFor(8L)).body()).isEqualTo("8");
	}

	@Test
	void hibernateAsksOurResolver() {
		Object resolver = this.entityManagerFactory.unwrap(SessionFactoryImplementor.class)
			.getCurrentTenantIdentifierResolver();

		assertThat(resolver).isNotNull();
		assertThat(resolver.getClass().getSimpleName()).isEqualTo("TenantIdentifierResolver");
	}

	private HttpResponse<String> get(String token) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/probe/tenant"))
			.header("Authorization", "Bearer " + token)
			.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String tokenFor(long tenantId) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject("1")
			.claim("tenant_id", tenantId)
			.issuedAt(now)
			.expiresAt(now.plusSeconds(60))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	@RestController
	static class ProbeController {

		@GetMapping("/api/probe/tenant")
		String tenant() {
			return String.valueOf(TenantContext.require());
		}

	}

}
