package com.proconsulta.backend.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtConfigTests {

	private static final String SECRET = "secreto-de-prueba-de-32-bytes-o-mas!";

	@Test
	void rejectsShortSecret() {
		assertThatThrownBy(() -> new JwtConfig("corto")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void decodesWhatItEncodes() {
		JwtConfig config = new JwtConfig(SECRET);
		String token = encode(config, "42");

		assertThat(config.jwtDecoder().decode(token).getSubject()).isEqualTo("42");
	}

	@Test
	void rejectsTokenSignedWithAnotherSecret() {
		String token = encode(new JwtConfig("otro-secreto-distinto-de-32-bytes-o-mas"), "42");

		assertThatThrownBy(() -> new JwtConfig(SECRET).jwtDecoder().decode(token)).isInstanceOf(JwtException.class);
	}

	private static String encode(JwtConfig config, String subject) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(subject).issuedAt(now).expiresAt(now.plusSeconds(60)).build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return config.jwtEncoder().encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

}
