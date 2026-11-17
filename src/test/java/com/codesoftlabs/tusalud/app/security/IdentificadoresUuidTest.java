package com.codesoftlabs.tusalud.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Especialidad;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import java.time.LocalDateTime;
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
class IdentificadoresUuidTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ServiceTestFixture fixture;

    private String token(Usuario usuario) {
        return "Bearer " + jwtUtils.generateToken(UsuarioPrincipal.from(usuario));
    }

    @Test
    void lasEntidadesSeIdentificanConUuidAleatorios() {
        Usuario usuario = fixture.usuario("uuid1", RolNombre.ROLE_PACIENTE);
        Usuario otro = fixture.usuario("uuid2", RolNombre.ROLE_PACIENTE);
        Especialidad especialidad = fixture.especialidad();
        Paciente paciente = fixture.paciente();
        Medico medico = fixture.medicoConHorarioLunes(java.math.BigDecimal.TEN);

        assertThat(usuario.getId()).isNotNull().isNotEqualTo(otro.getId());
        assertThat(usuario.getId().version()).isEqualTo(4);
        assertThat(especialidad.getId().version()).isEqualTo(4);
        assertThat(paciente.getId().version()).isEqualTo(4);
        assertThat(medico.getId().version()).isEqualTo(4);
    }

    @Test
    void lasRespuestasSerializanLosIdentificadoresComoTexto() throws Exception {
        Usuario paciente = fixture.usuario("uuid3", RolNombre.ROLE_PACIENTE);
        Especialidad especialidad = fixture.especialidad();

        mockMvc.perform(get("/api/v1/especialidades/" + especialidad.getId())
                        .header(HttpHeaders.AUTHORIZATION, token(paciente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(especialidad.getId().toString()));
    }

    @Test
    void unIdentificadorQueNoEsUuidResponde400ConElFormatoDeError() throws Exception {
        Usuario paciente = fixture.usuario("uuid4", RolNombre.ROLE_PACIENTE);

        mockMvc.perform(get("/api/v1/especialidades/123").header(HttpHeaders.AUTHORIZATION, token(paciente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("'id'")));
        mockMvc.perform(get("/api/v1/medicos/no-es-un-uuid").header(HttpHeaders.AUTHORIZATION, token(paciente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("no-es-un-uuid")));
        mockMvc.perform(get("/api/v1/medicos")
                        .param("especialidadId", "7")
                        .header(HttpHeaders.AUTHORIZATION, token(paciente)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unUuidInexistenteResponde404() throws Exception {
        Usuario paciente = fixture.usuario("uuid5", RolNombre.ROLE_PACIENTE);

        mockMvc.perform(get("/api/v1/especialidades/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, token(paciente)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unCuerpoConIdentificadorInvalidoResponde400() throws Exception {
        Usuario admin = fixture.usuario("uuid6", RolNombre.ROLE_ADMIN);

        mockMvc.perform(put("/api/v1/medicos/" + UUID.randomUUID() + "/especialidades")
                        .header(HttpHeaders.AUTHORIZATION, token(admin))
                        .contentType("application/json")
                        .content("{\"especialidadIds\":[1]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lasCitasTambienUsanUuid() {
        Especialidad especialidad = fixture.especialidad();
        Medico medico = fixture.medicoConHorarioLunes(java.math.BigDecimal.TEN);
        Paciente paciente = fixture.paciente();
        Cita cita = fixture.cita(medico, paciente, LocalDateTime.now().plusDays(2), EstadoCita.PENDIENTE);

        assertThat(cita.getId().version()).isEqualTo(4);
    }
}
