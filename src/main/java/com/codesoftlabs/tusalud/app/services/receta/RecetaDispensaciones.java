package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.RecetaDispensacion;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronicaItem;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.RecetaDispensacionRepository;
import com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RecetaDispensaciones implements RecetaDispensacionPuerto {

    private final RecetaElectronicaRepository recetas;
    private final RecetaDispensacionRepository dispensaciones;

    @Override
    @Transactional(readOnly = true)
    public void validar(Atencion atencion, int posicion, int cantidad) {
        List<RecetaElectronica> todas = recetas.findByAtencionIdOrderByEmitidaEnDesc(atencion.getId());
        if (todas.isEmpty()) {
            return;
        }
        RecetaElectronica receta = activa(todas)
                .orElseThrow(() -> new ConflictException(
                        "La receta electrónica " + todas.get(0).getNumero() + " fue anulada y no se puede dispensar"));
        if (LocalDateTime.now().isAfter(receta.getVigenteHasta())) {
            throw new ConflictException("La receta electrónica " + receta.getNumero() + " venció");
        }
        RecetaElectronicaItem item = item(receta, posicion);
        if (item == null) {
            throw new ConflictException(
                    "El ítem " + posicion + " no forma parte de la receta electrónica " + receta.getNumero());
        }
        if (cantidad > item.pendiente()) {
            throw new ConflictException("La cantidad de " + item.getMedicamentoTexto()
                    + " supera lo pendiente de la receta: " + item.pendiente());
        }
    }

    @Override
    @Transactional
    public boolean registrar(Atencion atencion, int posicion, int cantidad, UUID dispensacionId, UUID usuarioId) {
        List<RecetaElectronica> todas = recetas.findByAtencionIdOrderByEmitidaEnDesc(atencion.getId());
        Optional<RecetaElectronica> activa = activa(todas);
        if (activa.isEmpty()) {
            return true;
        }
        RecetaElectronica receta = activa.get();
        RecetaElectronicaItem item = item(receta, posicion);
        if (item == null) {
            return true;
        }
        item.setCantidadDispensada(item.getCantidadDispensada() + cantidad);
        dispensaciones.save(RecetaDispensacion.builder()
                .item(item)
                .cantidad(cantidad)
                .fecha(LocalDateTime.now())
                .usuarioId(usuarioId)
                .dispensacionId(dispensacionId)
                .build());
        recalcular(receta);
        return item.pendiente() == 0;
    }

    @Override
    @Transactional
    public void revertir(UUID dispensacionId) {
        for (RecetaDispensacion d : dispensaciones.vigentesDeLaDispensacion(dispensacionId)) {
            RecetaElectronicaItem item = d.getItem();
            item.setCantidadDispensada(Math.max(0, item.getCantidadDispensada() - d.getCantidad()));
            d.setRevertida(true);
            recalcular(item.getReceta());
        }
    }

    private Optional<RecetaElectronica> activa(List<RecetaElectronica> todas) {
        return todas.stream()
                .filter(r -> r.getEstado() != EstadoRecetaElectronica.ANULADA)
                .findFirst();
    }

    private RecetaElectronicaItem item(RecetaElectronica receta, int posicion) {
        return receta.getItems().stream()
                .filter(i -> i.getAtencionPosicion() != null && i.getAtencionPosicion() == posicion)
                .findFirst()
                .orElse(null);
    }

    private void recalcular(RecetaElectronica receta) {
        boolean completa = receta.getItems().stream().allMatch(i -> i.pendiente() == 0);
        boolean ninguna = receta.getItems().stream().allMatch(i -> i.getCantidadDispensada() == 0);
        if (completa) {
            receta.setEstado(EstadoRecetaElectronica.DISPENSADA);
        } else if (ninguna) {
            receta.setEstado(EstadoRecetaElectronica.EMITIDA);
        } else {
            receta.setEstado(EstadoRecetaElectronica.DISPENSADA_PARCIAL);
        }
    }
}
