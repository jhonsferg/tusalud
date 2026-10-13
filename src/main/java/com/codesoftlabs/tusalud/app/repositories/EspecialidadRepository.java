package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Especialidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("SELECT e FROM Especialidad e ORDER BY e.nombre ASC")
    List<Especialidad> findAllOrdenadas();

    @Query("SELECT COUNT(m) > 0 FROM Medico m WHERE m.especialidad.id = :especialidadId")
    boolean tieneMedicosAsignados(@Param("especialidadId") Long especialidadId);
}
