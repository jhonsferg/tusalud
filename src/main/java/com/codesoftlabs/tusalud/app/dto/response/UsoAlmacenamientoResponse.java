package com.codesoftlabs.tusalud.app.dto.response;

import java.util.List;

public record UsoAlmacenamientoResponse(long totalObjetos, long totalBytes, List<UsoBucket> buckets) {

    public record UsoBucket(String bucket, long objetos, long bytes, List<UsoProposito> propositos) {}

    public record UsoProposito(String proposito, long objetos, long bytes) {}
}
