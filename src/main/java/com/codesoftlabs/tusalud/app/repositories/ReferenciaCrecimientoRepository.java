package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ReferenciaCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenciaCrecimientoRepository extends JpaRepository<ReferenciaCrecimiento, UUID> {

    List<ReferenciaCrecimiento> findByIndicadorAndSexoOrderByEdadMeses(
            IndicadorCrecimiento indicador, SexoPaciente sexo);

    Optional<ReferenciaCrecimiento> findByIndicadorAndSexoAndEdadMeses(
            IndicadorCrecimiento indicador, SexoPaciente sexo, int edadMeses);
}
