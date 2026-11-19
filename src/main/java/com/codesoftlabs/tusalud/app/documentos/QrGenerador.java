package com.codesoftlabs.tusalud.app.documentos;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.util.Map;

public final class QrGenerador {

    private static final int BLANCO = 0xFFFFFF;
    private static final int NEGRO = 0x0F172A;

    private QrGenerador() {}

    public static BufferedImage generar(String contenido, int lado) {
        try {
            BitMatrix matriz = new QRCodeWriter()
                    .encode(
                            contenido,
                            BarcodeFormat.QR_CODE,
                            lado,
                            lado,
                            Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            BufferedImage imagen = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < lado; x++) {
                for (int y = 0; y < lado; y++) {
                    imagen.setRGB(x, y, matriz.get(x, y) ? NEGRO : BLANCO);
                }
            }
            return imagen;
        } catch (WriterException ex) {
            throw new IllegalStateException("No se pudo generar el código QR", ex);
        }
    }
}
