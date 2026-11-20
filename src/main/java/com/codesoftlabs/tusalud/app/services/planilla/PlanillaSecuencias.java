package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.entities.SecuenciaPlanilla;
import com.codesoftlabs.tusalud.app.repositories.SecuenciaPlanillaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PlanillaSecuencias {

    public static final String BOLETA = "BP";

    private final SecuenciaPlanillaRepository repositorio;
    private final TransactionTemplate nueva;

    public PlanillaSecuencias(SecuenciaPlanillaRepository repositorio, PlatformTransactionManager administrador) {
        this.repositorio = repositorio;
        this.nueva = new TransactionTemplate(administrador);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int siguiente(String tipo, int anio) {
        SecuenciaPlanilla secuencia = repositorio.bloquear(tipo, anio).orElse(null);
        if (secuencia == null) {
            asegurar(tipo, anio);
            secuencia = repositorio
                    .bloquear(tipo, anio)
                    .orElseThrow(() -> new IllegalStateException("No se pudo crear la secuencia de planilla"));
        }
        secuencia.setUltimo(secuencia.getUltimo() + 1);
        repositorio.saveAndFlush(secuencia);
        return secuencia.getUltimo();
    }

    public static String formatear(String tipo, int anio, int numero) {
        return String.format("%s-%d-%06d", tipo, anio, numero);
    }

    private void asegurar(String tipo, int anio) {
        try {
            nueva.executeWithoutResult(estado -> repositorio.saveAndFlush(new SecuenciaPlanilla(null, tipo, anio, 0)));
        } catch (DataIntegrityViolationException ex) {
            return;
        }
    }
}
