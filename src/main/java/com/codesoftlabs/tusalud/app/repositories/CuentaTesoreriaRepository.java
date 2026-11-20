package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoCuentaTesoreria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CuentaTesoreriaRepository extends JpaRepository<CuentaTesoreria, UUID> {

    @Query("SELECT c FROM CuentaTesoreria c JOIN FETCH c.cuentaContable ORDER BY c.tipo, c.nombre")
    List<CuentaTesoreria> todas();

    @Query(
            "SELECT c FROM CuentaTesoreria c JOIN FETCH c.cuentaContable WHERE c.activa = true ORDER BY c.tipo, c.nombre")
    List<CuentaTesoreria> activas();

    Optional<CuentaTesoreria> findFirstByTipoAndPredeterminadaTrueAndActivaTrue(TipoCuentaTesoreria tipo);

    boolean existsByNombreIgnoreCase(String nombre);

    List<CuentaTesoreria> findByPredeterminadaTrueAndTipo(TipoCuentaTesoreria tipo);
}
