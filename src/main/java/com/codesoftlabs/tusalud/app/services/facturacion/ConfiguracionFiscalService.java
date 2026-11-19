package com.codesoftlabs.tusalud.app.services.facturacion;

import com.codesoftlabs.tusalud.app.config.FacturacionProperties;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.ConfiguracionFiscalRequest;
import com.codesoftlabs.tusalud.app.dto.request.FacturacionRequests.SerieRequest;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.ConfiguracionFiscalResponse;
import com.codesoftlabs.tusalud.app.dto.response.FacturacionResponses.SerieResponse;
import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import com.codesoftlabs.tusalud.app.entities.SerieComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.facturacion.ValidadorDocumentos;
import com.codesoftlabs.tusalud.app.repositories.ConfiguracionFiscalRepository;
import com.codesoftlabs.tusalud.app.repositories.SerieComprobanteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConfiguracionFiscalService {

    private final ConfiguracionFiscalRepository configuraciones;
    private final SerieComprobanteRepository series;
    private final UsuarioRepository usuarios;
    private final FacturacionProperties propiedades;

    public String sede() {
        return propiedades.sedePorDefecto();
    }

    public Optional<ConfiguracionFiscal> vigente() {
        return configuraciones.findBySede(sede()).filter(ConfiguracionFiscal::isHabilitada);
    }

    public Optional<ConfiguracionFiscal> actual() {
        return configuraciones.findBySede(sede());
    }

    public ConfiguracionFiscal exigir() {
        return configuraciones
                .findBySede(sede())
                .orElseThrow(() -> new ConflictException("La configuración fiscal aún no está definida"));
    }

    public Optional<ConfiguracionFiscalResponse> obtener() {
        return actual().map(this::respuesta);
    }

    @Transactional
    public ConfiguracionFiscalResponse actualizar(UsuarioPrincipal actor, ConfiguracionFiscalRequest r) {
        if (!ValidadorDocumentos.rucValido(r.ruc())) {
            throw new DatosInvalidosException("El RUC del emisor no es válido");
        }
        if (r.proveedor() == ProveedorFiscal.PSE || r.proveedor() == ProveedorFiscal.OSE) {
            if (r.urlServicio() == null || r.urlServicio().isBlank()) {
                throw new DatosInvalidosException("Un OSE o PSE exige indicar la dirección del servicio");
            }
        }
        ConfiguracionFiscal c = actual().orElseGet(
                        () -> ConfiguracionFiscal.builder().sede(sede()).build());
        c.setHabilitada(r.habilitada());
        c.setRuc(r.ruc());
        c.setRazonSocial(r.razonSocial().trim());
        c.setNombreComercial(vacioANulo(r.nombreComercial()));
        c.setDireccion(r.direccion().trim());
        c.setUbigeo(r.ubigeo());
        c.setDepartamento(r.departamento().trim());
        c.setProvincia(r.provincia().trim());
        c.setDistrito(r.distrito().trim());
        c.setCodigoLocal(r.codigoLocal() == null ? "0000" : r.codigoLocal());
        c.setProveedor(r.proveedor());
        c.setModo(r.modo());
        c.setUrlServicio(vacioANulo(r.urlServicio()));
        c.setAfectacionServicios(r.afectacionServicios());
        c.setBoletasPorResumen(r.boletasPorResumen());
        c.setDiasLimiteBaja(r.diasLimiteBaja() == null ? 7 : r.diasLimiteBaja());
        c.setActualizadoEn(LocalDateTime.now());
        c.setActualizadoPor(usuarios.getReferenceById(actor.getId()));
        c = configuraciones.save(c);
        log.info("Configuración fiscal actualizada por {}", actor.getId());
        return respuesta(c);
    }

    public List<SerieResponse> listarSeries() {
        return series.findBySedeOrderBySerieAsc(sede()).stream()
                .map(SerieResponse::de)
                .toList();
    }

    @Transactional
    public SerieResponse crearSerie(SerieRequest r) {
        validarPrefijo(r.tipo(), r.serie());
        if (series.findBySedeAndSerie(sede(), r.serie()).isPresent()) {
            throw new ConflictException("La serie " + r.serie() + " ya existe");
        }
        SerieComprobante serie = series.save(SerieComprobante.builder()
                .sede(sede())
                .tipo(r.tipo())
                .serie(r.serie())
                .correlativoActual(r.correlativoInicial() == null ? 0 : r.correlativoInicial())
                .activa(true)
                .build());
        return SerieResponse.de(serie);
    }

    @Transactional
    public SerieResponse cambiarEstado(UUID id, boolean activa) {
        SerieComprobante serie =
                series.findById(id).orElseThrow(() -> new ResourceNotFoundException("Serie", "id", id));
        serie.setActiva(activa);
        return SerieResponse.de(serie);
    }

    @Transactional
    public int siguienteNumero(String serie) {
        SerieComprobante s = series.bloquear(sede(), serie)
                .orElseThrow(() -> new DatosInvalidosException("La serie " + serie + " no está registrada"));
        if (!s.isActiva()) {
            throw new ConflictException("La serie " + serie + " está desactivada");
        }
        s.setCorrelativoActual(s.getCorrelativoActual() + 1);
        return s.getCorrelativoActual();
    }

    @Transactional
    public void sincronizar(TipoCpe tipo, String serie, int numero) {
        SerieComprobante s = series.bloquear(sede(), serie).orElse(null);
        if (s == null) {
            series.save(SerieComprobante.builder()
                    .sede(sede())
                    .tipo(tipo)
                    .serie(serie)
                    .correlativoActual(numero)
                    .activa(true)
                    .build());
        } else if (s.getCorrelativoActual() < numero) {
            s.setCorrelativoActual(numero);
        }
    }

    public Optional<String> serieDeNota(TipoCpe tipoNota, TipoCpe referenciado) {
        String prefijo = (referenciado == TipoCpe.FACTURA ? "F" : "B") + "%";
        return series.activas(sede(), tipoNota, prefijo).stream()
                .map(SerieComprobante::getSerie)
                .findFirst();
    }

    private ConfiguracionFiscalResponse respuesta(ConfiguracionFiscal c) {
        boolean prueba = propiedades.certificado().ruta() == null
                || propiedades.certificado().ruta().isBlank();
        boolean sol = propiedades.sol().usuario() != null
                && !propiedades.sol().usuario().isBlank()
                && propiedades.sol().clave() != null
                && !propiedades.sol().clave().isBlank();
        return ConfiguracionFiscalResponse.de(c, prueba, sol);
    }

    private static void validarPrefijo(TipoCpe tipo, String serie) {
        boolean factura = serie.startsWith("F");
        boolean valido = switch (tipo) {
            case FACTURA -> factura;
            case BOLETA -> serie.startsWith("B");
            case NOTA_CREDITO, NOTA_DEBITO -> serie.startsWith("F") || serie.startsWith("B");
        };
        if (!valido) {
            throw new DatosInvalidosException("La serie no es compatible con el tipo de comprobante");
        }
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
