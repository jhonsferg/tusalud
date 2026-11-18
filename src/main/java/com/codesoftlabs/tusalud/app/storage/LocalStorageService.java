package com.codesoftlabs.tusalud.app.storage;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.storage.tipo", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Pattern SEGMENTO_VALIDO = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,199}");
    private static final String SUFIJO_TIPO = ".tipo";

    private final Path raiz;

    public LocalStorageService(StorageProperties propiedades) {
        String directorio = propiedades.directorioLocal();
        Path base = directorio == null || directorio.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "tusalud-storage")
                : Path.of(directorio);
        this.raiz = base.toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo preparar el directorio de almacenamiento local", ex);
        }
    }

    @Override
    public void guardar(String bucket, String clave, InputStream contenido, long tamano, String tipoContenido) {
        Path destino = resolver(bucket, clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(contenido, destino, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(rutaTipo(destino), tipoContenido == null ? "" : tipoContenido);
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo guardar el objeto", ex);
        }
    }

    @Override
    public ObjetoAlmacenado abrir(String bucket, String clave) {
        Path origen = resolver(bucket, clave);
        try {
            return new ObjetoAlmacenado(Files.newInputStream(origen), Files.size(origen), leerTipo(origen));
        } catch (java.nio.file.NoSuchFileException ex) {
            throw new ObjetoNoEncontradoException("El objeto no existe en el almacén", ex);
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo abrir el objeto", ex);
        }
    }

    @Override
    public ObjetoAlmacenado abrir(String bucket, String clave, long desde, long hasta) {
        Path origen = resolver(bucket, clave);
        try {
            long total = Files.size(origen);
            if (desde < 0 || hasta < desde || desde >= total) {
                throw new AlmacenamientoException("Rango fuera del objeto");
            }
            long fin = Math.min(hasta, total - 1);
            long longitud = fin - desde + 1;
            InputStream flujo = Files.newInputStream(origen);
            flujo.skipNBytes(desde);
            return new ObjetoAlmacenado(new Acotado(flujo, longitud), longitud, leerTipo(origen));
        } catch (java.nio.file.NoSuchFileException ex) {
            throw new ObjetoNoEncontradoException("El objeto no existe en el almacén", ex);
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo abrir el objeto", ex);
        }
    }

    @Override
    public void eliminar(String bucket, String clave) {
        Path destino = resolver(bucket, clave);
        try {
            Files.deleteIfExists(destino);
            Files.deleteIfExists(rutaTipo(destino));
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo eliminar el objeto", ex);
        }
    }

    @Override
    public boolean existe(String bucket, String clave) {
        return Files.isRegularFile(resolver(bucket, clave));
    }

    @Override
    public Optional<MetadatosObjeto> metadatos(String bucket, String clave) {
        Path origen = resolver(bucket, clave);
        if (!Files.isRegularFile(origen)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new MetadatosObjeto(Files.size(origen), leerTipo(origen)));
        } catch (IOException ex) {
            throw new AlmacenamientoException("No se pudo leer el objeto", ex);
        }
    }

    @Override
    public Optional<URI> urlPrefirmada(String bucket, String clave, Duration vigencia) {
        return Optional.empty();
    }

    private Path resolver(String bucket, String clave) {
        if (bucket == null
                || clave == null
                || !SEGMENTO_VALIDO.matcher(bucket).matches()
                || !SEGMENTO_VALIDO.matcher(clave).matches()
                || clave.contains("..")) {
            throw new AlmacenamientoException("Clave de objeto no válida");
        }
        Path ruta = raiz.resolve(bucket).resolve(clave).normalize();
        if (!ruta.startsWith(raiz)) {
            throw new AlmacenamientoException("Clave de objeto no válida");
        }
        return ruta;
    }

    private static final class Acotado extends FilterInputStream {

        private long restante;

        Acotado(InputStream flujo, long limite) {
            super(flujo);
            this.restante = limite;
        }

        @Override
        public int read() throws IOException {
            if (restante <= 0) {
                return -1;
            }
            int valor = super.read();
            if (valor >= 0) {
                restante--;
            }
            return valor;
        }

        @Override
        public int read(byte[] destino, int desplazamiento, int longitud) throws IOException {
            if (restante <= 0) {
                return -1;
            }
            int leidos = super.read(destino, desplazamiento, (int) Math.min(longitud, restante));
            if (leidos > 0) {
                restante -= leidos;
            }
            return leidos;
        }
    }

    private static Path rutaTipo(Path objeto) {
        return objeto.resolveSibling(objeto.getFileName() + SUFIJO_TIPO);
    }

    private static String leerTipo(Path objeto) throws IOException {
        Path ruta = rutaTipo(objeto);
        return Files.isRegularFile(ruta) ? Files.readString(ruta) : "application/octet-stream";
    }
}
