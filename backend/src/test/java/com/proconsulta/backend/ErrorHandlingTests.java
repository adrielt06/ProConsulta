package com.proconsulta.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

// Todavía no hay endpoints reales: ProbeController provoca cada error contra la base real,
// así se comprueban los nombres de restricción de V1__init.sql y no sólo el handler.
@Import({ TestcontainersConfiguration.class, ErrorHandlingTests.ProbeController.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ErrorHandlingTests {

	@LocalServerPort
	int port;

	@Test
	void invalidBodyIs400WithFieldErrors() throws Exception {
		HttpResponse<String> response = post("/validation", "{}");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"errors\"").contains("\"name\"");
	}

	@Test
	void missingResourceIs404() throws Exception {
		assertThat(post("/not-found", "").statusCode()).isEqualTo(404);
	}

	@Test
	void duplicateSlugIs409() throws Exception {
		HttpResponse<String> response = post("/duplicate-slug", "");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("identificador de consultorio");
	}

	@Test
	void overlappingAppointmentIs409() throws Exception {
		HttpResponse<String> response = post("/overlap", "");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("ya tiene un turno");
	}

	@Test
	void otherIntegrityViolationIs400WithoutDatabaseDetails() throws Exception {
		HttpResponse<String> response = post("/bad-slug", "");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).doesNotContain("tenant_slug_check").doesNotContain("constraint");
	}

	private HttpResponse<String> post(String path, String json) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/api/public/probe" + path))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json))
			.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	record Body(@NotBlank String name) {
	}

	// Bajo /api/public para que pase la seguridad sin token.
	@RestController
	@RequestMapping("/api/public/probe")
	static class ProbeController {

		private final JdbcTemplate jdbc;

		ProbeController(JdbcTemplate jdbc) {
			this.jdbc = jdbc;
		}

		@PostMapping("/validation")
		void validation(@Valid @RequestBody Body body) {
		}

		@PostMapping("/not-found")
		void notFound() {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		@PostMapping("/duplicate-slug")
		void duplicateSlug() {
			this.jdbc.update("INSERT INTO tenant (slug, name) VALUES ('probe-dup', 'A')");
			this.jdbc.update("INSERT INTO tenant (slug, name) VALUES ('probe-dup', 'B')");
		}

		@PostMapping("/bad-slug")
		void badSlug() {
			this.jdbc.update("INSERT INTO tenant (slug, name) VALUES ('NO VALIDO', 'A')");
		}

		@PostMapping("/overlap")
		void overlap() {
			Long tenant = this.jdbc.queryForObject(
					"INSERT INTO tenant (slug, name) VALUES ('probe-overlap', 'A') RETURNING id", Long.class);
			Long professional = this.jdbc.queryForObject(
					"INSERT INTO professional (tenant_id, display_name) VALUES (?, 'Lic. A') RETURNING id", Long.class,
					tenant);
			Long patient = this.jdbc.queryForObject("""
					INSERT INTO patient (tenant_id, first_name, last_name, dni, email)
					VALUES (?, 'Ana', 'Perez', '12345678', 'ana@example.com') RETURNING id""", Long.class, tenant);
			Long service = this.jdbc.queryForObject("""
					INSERT INTO service (tenant_id, name, duration_minutes, price)
					VALUES (?, 'Consulta', 60, 100) RETURNING id""", Long.class, tenant);
			String appointment = """
					INSERT INTO appointment (tenant_id, professional_id, patient_id, service_id,
					                         starts_at, ends_at, service_name, price)
					VALUES (?, ?, ?, ?, ?::timestamptz, ?::timestamptz, 'Consulta', 100)""";
			this.jdbc.update(appointment, tenant, professional, patient, service, "2026-11-02T10:00:00Z",
					"2026-11-02T11:00:00Z");
			this.jdbc.update(appointment, tenant, professional, patient, service, "2026-11-02T10:30:00Z",
					"2026-11-02T11:30:00Z");
		}

	}

}
