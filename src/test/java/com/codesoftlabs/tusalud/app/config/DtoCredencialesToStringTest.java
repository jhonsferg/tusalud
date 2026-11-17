package com.codesoftlabs.tusalud.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.dto.request.LoginRequest;
import com.codesoftlabs.tusalud.app.dto.request.MfaCodigoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PasswordChangeRequest;
import com.codesoftlabs.tusalud.app.dto.request.RefreshTokenRequest;
import org.junit.jupiter.api.Test;

class DtoCredencialesToStringTest {

    @Test
    void losDtoConCredencialesNoImprimenSusValores() {
        assertThat(new LoginRequest("a@b.com", "Secreta123!").toString())
                .doesNotContain("Secreta123!")
                .doesNotContain("a@b.com");
        assertThat(new PasswordChangeRequest("Actual123!", "Nueva456!").toString())
                .doesNotContain("Actual123!")
                .doesNotContain("Nueva456!");
        assertThat(new RefreshTokenRequest("token-largo").toString()).doesNotContain("token-largo");
        assertThat(new MfaCodigoRequest("123456").toString()).doesNotContain("123456");
    }
}
