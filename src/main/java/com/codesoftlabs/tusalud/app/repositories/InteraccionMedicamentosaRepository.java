package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.InteraccionMedicamentosa;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InteraccionMedicamentosaRepository extends JpaRepository<InteraccionMedicamentosa, UUID> {

    @Query("""
            SELECT i FROM InteraccionMedicamentosa i JOIN FETCH i.principioA JOIN FETCH i.principioB
            WHERE i.principioA.id IN :ids AND i.principioB.id IN :ids
            """)
    List<InteraccionMedicamentosa> entre(@Param("ids") Collection<UUID> ids);

    @Query("""
            SELECT i FROM InteraccionMedicamentosa i JOIN FETCH i.principioA JOIN FETCH i.principioB
            ORDER BY i.principioA.dci ASC, i.principioB.dci ASC
            """)
    List<InteraccionMedicamentosa> listar();

    Optional<InteraccionMedicamentosa> findByPrincipioAIdAndPrincipioBId(UUID a, UUID b);
}
