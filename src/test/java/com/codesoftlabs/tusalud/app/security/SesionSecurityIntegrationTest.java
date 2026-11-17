package com.codesoftlabs.tusalud.app.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class SesionSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private SesionService sesionService;

    @Autowired
    private ServiceTestFixture fixture;

    private String tokenDeSesion(Usuario usuario, UUID tokenId) {
        return jwtUtils.generateToken(UsuarioPrincipal.from(usuario), tokenId);
    }

    @Test
    void tokenDeSesionRevocadaRecibe401() throws Exception {
        Usuario usuario = fixture.usuario("secsesion", RolNombre.ROLE_PACIENTE);
        UUID tokenId = sesionService.iniciarSesion(usuario, new ContextoConexion("1.1.1.1", "JUnit"));
        String token = tokenDeSesion(usuario, tokenId);

        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        sesionService.cerrarSesion(tokenId);

        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("La sesión no es válida o fue cerrada"));
    }

    @Test
    void logoutRevocaLaSesionActual() throws Exception {
        Usuario usuario = fixture.usuario("logout", RolNombre.ROLE_PACIENTE);
        UUID tokenId = sesionService.iniciarSesion(usuario, new ContextoConexion("1.1.1.1", "JUnit"));
        String token = tokenDeSesion(usuario, tokenId);

        mockMvc.perform(post("/api/v1/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSinAutenticarRecibe401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void listadoDeSesionesMarcaLaSesionDelToken() throws Exception {
        Usuario usuario = fixture.usuario("listasesion", RolNombre.ROLE_PACIENTE);
        UUID tokenId = sesionService.iniciarSesion(usuario, new ContextoConexion("1.1.1.1", "JUnit"));
        String token = tokenDeSesion(usuario, tokenId);

        mockMvc.perform(get("/api/v1/perfil/sesiones").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actual").value(true))
                .andExpect(jsonPath("$[0].ip").value("1.1.1.1"));
    }
}
