package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.MetodoPagoRequest;
import com.codesoftlabs.tusalud.app.dto.response.MetodoPagoResponse;
import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.MarcaTarjeta;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.MetodoPagoRepository;
import com.codesoftlabs.tusalud.app.repositories.PagoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.MetodoPagoService;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetodoPagoServiceImpl implements MetodoPagoService {

    private static final int DIGITOS_MINIMOS = 13;
    private static final int DIGITOS_MAXIMOS = 19;

    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;

    @Override
    public List<MetodoPagoResponse> listar(UsuarioPrincipal actor) {
        YearMonth hoy = YearMonth.now();
        return metodoPagoRepository.findByUsuarioIdOrderByPredeterminadoDescIdDesc(actor.getId()).stream()
                .map(metodo -> MetodoPagoResponse.from(metodo, hoy))
                .toList();
    }

    @Override
    @Transactional
    public MetodoPagoResponse crear(UsuarioPrincipal actor, MetodoPagoRequest request) {
        String numero = request.numeroTarjeta().replaceAll("[^0-9]", "");
        validarNumero(numero);
        if (YearMonth.of(request.vencimientoAnio(), request.vencimientoMes()).isBefore(YearMonth.now())) {
            throw new DatosInvalidosException("La tarjeta está vencida");
        }
        Usuario usuario = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));

        List<MetodoPago> existentes =
                metodoPagoRepository.findByUsuarioIdOrderByPredeterminadoDescIdDesc(actor.getId());
        boolean predeterminado = existentes.isEmpty() || Boolean.TRUE.equals(request.predeterminado());
        if (predeterminado) {
            existentes.forEach(metodo -> metodo.setPredeterminado(false));
        }

        MetodoPago metodo = MetodoPago.builder()
                .usuario(usuario)
                .tipo(request.tipo())
                .titular(request.titular().trim())
                .marca(detectarMarca(numero))
                .ultimos4(numero.substring(numero.length() - 4))
                .vencimientoMes(request.vencimientoMes())
                .vencimientoAnio(request.vencimientoAnio())
                .predeterminado(predeterminado)
                .build();
        return MetodoPagoResponse.from(metodoPagoRepository.save(metodo), YearMonth.now());
    }

    @Override
    @Transactional
    public MetodoPagoResponse marcarPredeterminado(UsuarioPrincipal actor, Long id) {
        List<MetodoPago> metodos = metodoPagoRepository.findByUsuarioIdOrderByPredeterminadoDescIdDesc(actor.getId());
        MetodoPago objetivo = buscar(metodos, id);
        metodos.forEach(metodo -> metodo.setPredeterminado(metodo.getId().equals(id)));
        return MetodoPagoResponse.from(objetivo, YearMonth.now());
    }

    @Override
    @Transactional
    public void eliminar(UsuarioPrincipal actor, Long id) {
        List<MetodoPago> metodos = metodoPagoRepository.findByUsuarioIdOrderByPredeterminadoDescIdDesc(actor.getId());
        MetodoPago metodo = buscar(metodos, id);
        boolean eraPredeterminado = metodo.isPredeterminado();
        pagoRepository.desvincularMetodo(id);
        metodoPagoRepository.delete(metodo);
        if (eraPredeterminado) {
            metodos.stream()
                    .filter(otro -> !otro.getId().equals(id))
                    .findFirst()
                    .ifPresent(siguiente -> siguiente.setPredeterminado(true));
        }
    }

    private static MetodoPago buscar(List<MetodoPago> metodos, Long id) {
        return metodos.stream()
                .filter(metodo -> metodo.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Método de pago", "id", id));
    }

    private static void validarNumero(String numero) {
        if (numero.length() < DIGITOS_MINIMOS || numero.length() > DIGITOS_MAXIMOS) {
            throw new DatosInvalidosException("El número de tarjeta debe tener entre 13 y 19 dígitos");
        }
        if (!cumpleLuhn(numero)) {
            throw new DatosInvalidosException("El número de tarjeta no es válido");
        }
    }

    private static boolean cumpleLuhn(String numero) {
        int suma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int digito = numero.charAt(i) - '0';
            if (duplicar) {
                digito *= 2;
                if (digito > 9) {
                    digito -= 9;
                }
            }
            suma += digito;
            duplicar = !duplicar;
        }
        return suma % 10 == 0;
    }

    private static MarcaTarjeta detectarMarca(String numero) {
        if (numero.startsWith("4")) {
            return MarcaTarjeta.VISA;
        }
        if (numero.startsWith("34") || numero.startsWith("37")) {
            return MarcaTarjeta.AMEX;
        }
        int dosPrimeros = Integer.parseInt(numero.substring(0, 2));
        if (dosPrimeros >= 51 && dosPrimeros <= 55) {
            return MarcaTarjeta.MASTERCARD;
        }
        if (numero.length() >= 4) {
            int cuatroPrimeros = Integer.parseInt(numero.substring(0, 4));
            if (cuatroPrimeros >= 2221 && cuatroPrimeros <= 2720) {
                return MarcaTarjeta.MASTERCARD;
            }
        }
        return MarcaTarjeta.OTRA;
    }
}
