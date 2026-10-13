package com.proconsulta.backend.shared;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

// Le dice a Hibernate cuál es el consultorio actual; con eso agrega solo el
// "tenant_id = ?" a cada consulta sobre una entidad con @TenantId.
@Component
class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

	// Los ids de tenant empiezan en 1, así que 0 no coincide con ninguna fila. Sin
	// consultorio en contexto las lecturas vuelven vacías y las escrituras fallan por la
	// clave foránea. No se devuelve null porque Hibernate se negaría a abrir la sesión, y
	// register y login necesitan abrirla antes de conocer el consultorio.
	static final long NO_TENANT = 0L;

	@Override
	public Long resolveCurrentTenantIdentifier() {
		Long tenantId = TenantContext.currentOrNull();
		return (tenantId != null) ? tenantId : NO_TENANT;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

	@Override
	public void customize(Map<String, Object> hibernateProperties) {
		hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
	}

}
