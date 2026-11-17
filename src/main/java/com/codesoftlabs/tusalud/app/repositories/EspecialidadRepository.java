package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, UUID id);

    @Query("SELECT e FROM Especialidad e ORDER BY e.nombre ASC")
    List<Especialidad> findAllOrdenadas();

    @Query("SELECT COUNT(m) > 0 FROM Medico m JOIN m.especialidades e WHERE e.id = :especialidadId")
    boolean tieneMedicosAsignados(@Param("especialidadId") UUID especialidadId);

    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.especialidad.id = :especialidadId")
    boolean tieneCitas(@Param("especialidadId") UUID especialidadId);
}
