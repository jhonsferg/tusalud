package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.BancoDatos;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BancoDatosRepository extends JpaRepository<BancoDatos, UUID> {

    List<BancoDatos> findAllByOrderByNombreAsc();

    boolean existsByCodigo(String codigo);
}
