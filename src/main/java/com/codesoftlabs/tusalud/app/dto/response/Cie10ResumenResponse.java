package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record Cie10ResumenResponse(
        long totalActivos,
        long totalMuestra,
        boolean soloMuestra,
        Cie10VersionResponse versionVigente,
        List<Cie10VersionResponse> versiones) {}
