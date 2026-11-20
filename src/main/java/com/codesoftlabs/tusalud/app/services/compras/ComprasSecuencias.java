package com.codesoftlabs.tusalud.app.services.compras;

import com.codesoftlabs.tusalud.app.entities.SecuenciaCompra;
import com.codesoftlabs.tusalud.app.repositories.SecuenciaCompraRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ComprasSecuencias {

    public static final String REQUISICION = "REQ";
    public static final String ORDEN = "OC";
    public static final String RECEPCION = "REC";
    public static final String DEVOLUCION = "DEV";

    private final SecuenciaCompraRepository repositorio;
    private final TransactionTemplate nueva;

    public ComprasSecuencias(SecuenciaCompraRepository repositorio, PlatformTransactionManager administrador) {
        this.repositorio = repositorio;
        this.nueva = new TransactionTemplate(administrador);
        this.nueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int siguiente(String tipo, int anio) {
        SecuenciaCompra secuencia = repositorio.bloquear(tipo, anio).orElse(null);
        if (secuencia == null) {
            asegurar(tipo, anio);
            secuencia = repositorio
                    .bloquear(tipo, anio)
                    .orElseThrow(() -> new IllegalStateException("No se pudo crear la secuencia de compras"));
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
            nueva.executeWithoutResult(estado -> repositorio.saveAndFlush(new SecuenciaCompra(null, tipo, anio, 0)));
        } catch (DataIntegrityViolationException ex) {
            return;
        }
    }
}
