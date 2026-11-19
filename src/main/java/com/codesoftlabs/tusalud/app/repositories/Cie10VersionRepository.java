package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cie10Version;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Cie10VersionRepository extends JpaRepository<Cie10Version, UUID> {

    List<Cie10Version> findAllByOrderByCreadaEnDesc();
}
