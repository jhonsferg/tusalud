package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroAnulado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CobroRegistrado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraAnulada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.CompraRegistrada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PagoProveedorRegistrado;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaAprobada;
import com.codesoftlabs.tusalud.app.services.events.HechosContables.PlanillaPagada;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EscuchaHechosContables {

    private final AsientosPendientesService despachador;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRegistrarCobro(CobroRegistrado hecho) {
        despachador.despachar(hecho, hecho.comprobanteId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAnularCobro(CobroAnulado hecho) {
        despachador.despachar(hecho, hecho.comprobanteId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alRegistrarCompra(CompraRegistrada hecho) {
        despachador.despachar(hecho, hecho.comprobanteId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAnularCompra(CompraAnulada hecho) {
        despachador.despachar(hecho, hecho.comprobanteId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alPagarProveedor(PagoProveedorRegistrado hecho) {
        despachador.despachar(hecho, hecho.pagoId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alAprobarPlanilla(PlanillaAprobada hecho) {
        despachador.despachar(hecho, hecho.planillaId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void alPagarPlanilla(PlanillaPagada hecho) {
        despachador.despachar(hecho, hecho.planillaId());
    }
}
