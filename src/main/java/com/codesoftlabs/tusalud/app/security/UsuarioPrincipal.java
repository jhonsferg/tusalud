package com.codesoftlabs.tusalud.app.security;

import com.codesoftlabs.tusalud.app.entities.Usuario;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UsuarioPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String nombreCompleto;
    private final boolean activo;
    private final List<GrantedAuthority> authorities;

    private UsuarioPrincipal(
            Long id,
            String email,
            String password,
            String nombreCompleto,
            boolean activo,
            List<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.activo = activo;
        this.authorities = authorities;
    }

    public static UsuarioPrincipal from(Usuario usuario) {
        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> (GrantedAuthority)
                        new SimpleGrantedAuthority(rol.getNombre().name()))
                .toList();
        return new UsuarioPrincipal(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.getNombreCompleto(),
                usuario.isActivo(),
                authorities);
    }

    public List<String> getRoles() {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
