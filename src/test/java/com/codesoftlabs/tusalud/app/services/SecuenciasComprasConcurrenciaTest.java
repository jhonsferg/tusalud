package com.codesoftlabs.tusalud.app.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.codesoftlabs.tusalud.app.services.compras.ComprasSecuencias;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class SecuenciasComprasConcurrenciaTest {

    private static final int HILOS = 8;

    @Autowired
    private ComprasSecuencias secuencias;

    @Autowired
    private PlatformTransactionManager administrador;

    @Test
    void variosHilosObtienenNumerosDistintosYSinHuecos() throws Exception {
        int anio = 2998;
        Set<Integer> numeros = ConcurrentHashMap.newKeySet();
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch listos = new CountDownLatch(HILOS);
        ExecutorService ejecutor = Executors.newFixedThreadPool(HILOS);
        TransactionTemplate transaccion = new TransactionTemplate(administrador);
        for (int i = 0; i < HILOS; i++) {
            ejecutor.submit(() -> {
                listos.countDown();
                try {
                    salida.await();
                    Integer numero = transaccion.execute(estado -> secuencias.siguiente(ComprasSecuencias.ORDEN, anio));
                    numeros.add(numero);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        listos.await(10, TimeUnit.SECONDS);
        salida.countDown();
        ejecutor.shutdown();
        assertThat(ejecutor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(numeros).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8);
        assertThat(ComprasSecuencias.formatear(ComprasSecuencias.ORDEN, anio, 8))
                .isEqualTo("OC-2998-000008");
    }
}
