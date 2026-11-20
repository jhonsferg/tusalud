package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.GuiaClinica;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoGuia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuiaClinicaRepository extends JpaRepository<GuiaClinica, UUID> {

    @Query("SELECT g FROM GuiaClinica g LEFT JOIN FETCH g.especialidad ORDER BY g.titulo ASC, g.version DESC")
    List<GuiaClinica> todas();

    @Query("SELECT g FROM GuiaClinica g LEFT JOIN FETCH g.especialidad WHERE g.id = :id")
    Optional<GuiaClinica> detalle(@Param("id") UUID id);

    @Query(
            "SELECT g FROM GuiaClinica g LEFT JOIN FETCH g.especialidad WHERE g.codigo = :codigo ORDER BY g.version DESC")
    List<GuiaClinica> versiones(@Param("codigo") String codigo);

    @Query("SELECT g FROM GuiaClinica g LEFT JOIN FETCH g.especialidad WHERE g.estado = :vigente")
    List<GuiaClinica> vigentes(@Param("vigente") EstadoGuia vigente);
}
