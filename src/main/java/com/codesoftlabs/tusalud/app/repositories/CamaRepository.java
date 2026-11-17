package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CamaRepository extends JpaRepository<Cama, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Cama> findByCodigoIgnoreCase(String codigo);

    List<Cama> findAllByOrderByAreaAscCodigoAsc();

    long countByActivaTrue();

    long countByActivaTrueAndEstado(EstadoCama estado);
}
