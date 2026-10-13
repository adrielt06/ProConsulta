package com.proconsulta.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

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

// HTTP real y no MockMvc: MockMvc no hace el reenvío a /error, que es parte de lo que se prueba.
// Todavía no hay controladores, así que "pasó la seguridad" se ve como 404.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityTests {

	@LocalServerPort
	int port;

	@Autowired
	JwtEncoder jwtEncoder;

	@Test
	void privateRouteWithoutTokenIs401() throws Exception {
		assertThat(status("GET", "/api/patients", null)).isEqualTo(401);
	}

	@Test
	void privateRouteWithInvalidTokenIs401() throws Exception {
		assertThat(status("GET", "/api/patients", "no-es-un-jwt")).isEqualTo(401);
	}

	@Test
	void privateRouteWithValidTokenPassesSecurity() throws Exception {
		assertThat(status("GET", "/api/patients", validToken())).isEqualTo(404);
	}

	@Test
	void publicRoutesPassSecurityWithoutToken() throws Exception {
		assertThat(status("GET", "/api/public/nutri-ana", null)).isEqualTo(404);
		assertThat(status("POST", "/api/auth/login", null)).isEqualTo(404);
	}

	@Test
	void publicRouteIgnoresInvalidToken() throws Exception {
		assertThat(status("POST", "/api/auth/refresh", "no-es-un-jwt")).isEqualTo(404);
	}

	private int status(String method, String path, String bearer) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path))
			.method(method, HttpRequest.BodyPublishers.noBody());
		if (bearer != null) {
			request.header("Authorization", "Bearer " + bearer);
		}
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
	}

	private String validToken() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").issuedAt(now).expiresAt(now.plusSeconds(60)).build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return this.jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
