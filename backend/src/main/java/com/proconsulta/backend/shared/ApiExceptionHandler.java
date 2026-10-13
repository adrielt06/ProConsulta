package com.proconsulta.backend.shared;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Todas las respuestas de error salen como ProblemDetail. La clase base ya cubre los errores
// estándar de Spring MVC (JSON mal formado, ruta inexistente, ResponseStatusException);
// acá se agregan el detalle por campo del 400 y los 409 que nacen en la base.
// Para un 404 de negocio: throw new ResponseStatusException(HttpStatus.NOT_FOUND).
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String UNIQUE_VIOLATION = "23505";

	private static final String EXCLUSION_VIOLATION = "23P01";

	// Nombre de la restricción en V1__init.sql -> mensaje para el usuario.
	private static final Map<String, String> CONFLICTS = Map.of(
			"app_user_email_uk", "Ya existe una cuenta con ese email",
			"tenant_slug_key", "Ese identificador de consultorio ya está en uso",
			"patient_dni_uk", "Ya existe un paciente con ese DNI",
			"service_name_uk", "Ya existe un servicio con ese nombre",
			"health_insurer_name_uk", "Ya existe una obra social con ese nombre",
			"appointment_no_overlap", "El profesional ya tiene un turno en ese horario");

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
		SQLException sql = sqlCause(ex);
		String state = (sql != null) ? sql.getSQLState() : null;
		if (UNIQUE_VIOLATION.equals(state) || EXCLUSION_VIOLATION.equals(state)) {
			String message = String.valueOf(sql.getMessage());
			String detail = CONFLICTS.entrySet()
				.stream()
				.filter(conflict -> message.contains('"' + conflict.getKey() + '"'))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElse("El dato ya existe");
			return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
		}
		// Clave foránea, CHECK o NOT NULL: entrada que la validación dejó pasar.
		// Se registra el detalle y no se le muestra al cliente el mensaje de la base.
		logger.warn("Violación de integridad no traducida", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Los datos no cumplen una restricción");
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult()
			.getFieldErrors()
			.forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		ProblemDetail body = ex.getBody();
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	private static SQLException sqlCause(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sql) {
				return sql;
			}
		}
		return null;
	}

}
