package com.codesoftlabs.tusalud.app.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.services.ServiceTestFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ServiceTestFixture.class)
class LoginFallidoSinTransaccionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceTestFixture fixture;

    @Autowired
    private JdbcTemplate jdbc;

    private ResultActions login(String email, String clave) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}"));
    }

    @Test
    void unaCuentaInexistenteRespondeUnauthorizedAunSinTransaccionExterna() throws Exception {
        String email = "nadie.sin.transaccion@tusalud.test";
        for (int i = 0; i < 2; i++) {
            login(email, "Incorrecta123!")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message", is("Credenciales inválidas")))
                    .andExpect(jsonPath("$.message", not(containsString(email))));
        }
    }

    @Test
    void elLoginExitosoYElFallidoFuncionanSinTransaccionExterna() throws Exception {
        Usuario usuario = fixture.usuario("login-real", RolNombre.ROLE_CAJA);
        try {
            login(usuario.getEmail(), "Incorrecta123!").andExpect(status().isUnauthorized());
            login(usuario.getEmail(), ServiceTestFixture.PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.email", is(usuario.getEmail())));
            Integer eventos = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM eventos_seguridad WHERE usuario_id = ?", Integer.class, usuario.getId());
            org.assertj.core.api.Assertions.assertThat(eventos).isEqualTo(1);
        } finally {
            jdbc.update("DELETE FROM eventos_seguridad WHERE usuario_id = ?", usuario.getId());
            jdbc.update("DELETE FROM refresh_tokens WHERE usuario_id = ?", usuario.getId());
            jdbc.update("DELETE FROM sesiones_usuario WHERE usuario_id = ?", usuario.getId());
            jdbc.update("DELETE FROM notificaciones WHERE usuario_id = ?", usuario.getId());
            jdbc.update("DELETE FROM usuario_roles WHERE usuario_id = ?", usuario.getId());
            jdbc.update("DELETE FROM usuarios WHERE id = ?", usuario.getId());
        }
    }
}
