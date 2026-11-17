package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.response.AuthResponse;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import com.codesoftlabs.tusalud.app.services.interfaces.AuthService;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(ServiceTestFixture.class)
class RefreshYCalendarioHttpTest {

    private static final ContextoConexion CONTEXTO = new ContextoConexion("10.0.0.7", "JUnit");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private AuthResponse iniciarSesion() {
        Usuario usuario = fixture.usuario("httprefresh", RolNombre.ROLE_PACIENTE);
        return authService.login(new LoginRequest(usuario.getEmail(), ServiceTestFixture.PASSWORD), CONTEXTO);
    }

    @Test
    void refreshEsPublicoYRotaElToken() throws Exception {
        AuthResponse sesion = iniciarSesion();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + sesion.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken", not(sesion.refreshToken())))
                .andExpect(jsonPath("$.refreshExpiresIn").value(jwtUtils.getRefreshExpirationMs()))
                .andExpect(jsonPath("$.mfaToken").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + sesion.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRechazaCuerposInvalidosYTokensDesconocidos() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"desconocido\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenMfaNoSirveComoBearerNiComoRefresh() throws Exception {
        Usuario usuario = fixture.usuario("httpmfa", RolNombre.ROLE_PACIENTE);
        String mfa = jwtUtils.generateMfaToken(UsuarioPrincipal.from(usuario));

        mockMvc.perform(get("/api/v1/perfil").header("Authorization", "Bearer " + mfa))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + mfa + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disponibilidadPorRangoExigeAutenticacionYValidaElRango() throws Exception {
        Medico medico = fixture.medicoConHorarioLunes(new BigDecimal("90.00"));
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        String url = "/api/v1/medicos/" + medico.getId() + "/disponibilidad/rango";

        mockMvc.perform(get(url).param("inicio", lunes.toString()).param("fin", lunes.toString()))
                .andExpect(status().isUnauthorized());

        String token = iniciarSesion().token();
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .param("inicio", lunes.toString())
                        .param("fin", lunes.plusDays(6).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fecha").value(lunes.toString()))
                .andExpect(jsonPath("$[0].totalHoras").value(8))
                .andExpect(jsonPath("$[0].horariosDisponibles[0]").value("08:00:00"));
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .param("inicio", lunes.toString())
                        .param("fin", lunes.plusDays(62).toString()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url).header("Authorization", "Bearer " + token)
                        .param("inicio", lunes.toString())
                        .param("fin", lunes.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }
}
