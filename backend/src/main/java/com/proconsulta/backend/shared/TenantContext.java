package com.proconsulta.backend.shared;

/**
 * El consultorio del request en curso. Lo completa y lo limpia {@link TenantFilter}; el
 * resto del código sólo lo lee.
 */
public final class TenantContext {

	// Por hilo: Tomcat atiende cada request en un hilo y después lo reutiliza. Por eso
	// quien llama a set() está obligado a llamar a clear() en un finally.
	private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

	private TenantContext() {
	}

	/** El consultorio actual. Falla si no hay: nunca se opera "sin consultorio". */
	public static long require() {
		Long tenantId = CURRENT.get();
		if (tenantId == null) {
			throw new IllegalStateException("No hay consultorio en el contexto del request");
		}
		return tenantId;
	}

	static Long currentOrNull() {
		return CURRENT.get();
	}

	static void set(long tenantId) {
		CURRENT.set(tenantId);
	}

	static void clear() {
		CURRENT.remove();
	}

}
