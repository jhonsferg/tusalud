package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.entities.SecuenciaDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import com.codesoftlabs.tusalud.app.repositories.SecuenciaDocumentoMedicoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SecuenciasDocumentosMedicos {

    private final SecuenciaDocumentoMedicoRepository repositorio;
    private final TransactionTemplate nueva;

    public SecuenciasDocumentosMedicos(
            SecuenciaDocumentoMedicoRepository repositorio, PlatformTransactionManager administrador) {
        this.repositorio = repositorio;
        this.nueva = new TransactionTemplate(administrador);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int siguiente(TipoDocumentoMedico tipo, int anio) {
        SecuenciaDocumentoMedico secuencia =
                repositorio.bloquear(tipo.name(), anio).orElse(null);
        if (secuencia == null) {
            asegurar(tipo, anio);
            secuencia = repositorio
                    .bloquear(tipo.name(), anio)
                    .orElseThrow(() -> new IllegalStateException("No se pudo crear la secuencia del documento"));
        }
        secuencia.setUltimo(secuencia.getUltimo() + 1);
        repositorio.saveAndFlush(secuencia);
        return secuencia.getUltimo();
    }

    private void asegurar(TipoDocumentoMedico tipo, int anio) {
        try {
            nueva.executeWithoutResult(
                    estado -> repositorio.saveAndFlush(new SecuenciaDocumentoMedico(null, tipo.name(), anio, 0)));
        } catch (DataIntegrityViolationException ex) {
            return;
        }
    }
}
