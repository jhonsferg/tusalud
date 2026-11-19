package com.codesoftlabs.tusalud.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record IceServersResponse(List<IceServer> iceServers, long expiraEpochSegundos) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IceServer(List<String> urls, String username, String credential) {}
}
