package de.henzeob.inventory.application;

import jakarta.enterprise.context.ApplicationScoped;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

/**
 * Re-encodes uploaded images as JPEG, iteratively lowering quality (and, if that isn't enough,
 * downscaling) until the result fits under the target size. PNGs are converted rather than
 * compressed losslessly, since arbitrary size targets aren't achievable with lossless encoding.
 */
@ApplicationScoped
public class ImageCompressor {

    private static final long TARGET_BYTES = 500 * 1024L;
    private static final float MIN_QUALITY = 0.1f;
    private static final int MIN_DIMENSION = 200;

    public byte[] compressToJpeg(byte[] input) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(input));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (image == null) {
            throw new IllegalArgumentException("Unsupported or corrupt image data");
        }

        BufferedImage current = withoutAlpha(image);
        byte[] best = encodeJpeg(current, 0.9f);
        if (best.length <= TARGET_BYTES) {
            return best;
        }

        for (float quality = 0.8f; quality >= MIN_QUALITY; quality -= 0.1f) {
            byte[] attempt = encodeJpeg(current, quality);
            if (attempt.length <= TARGET_BYTES) {
                return attempt;
            }
            best = attempt;
        }

        while (current.getWidth() > MIN_DIMENSION && current.getHeight() > MIN_DIMENSION) {
            current = scaleDown(current, 0.75);
            byte[] attempt = encodeJpeg(current, MIN_QUALITY);
            if (attempt.length <= TARGET_BYTES) {
                return attempt;
            }
            best = attempt;
        }

        return best;
    }

    private BufferedImage withoutAlpha(BufferedImage source) {
        if (!source.getColorModel().hasAlpha()) {
            return source;
        }
        BufferedImage opaque = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        opaque.createGraphics().drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        return opaque;
    }

    private BufferedImage scaleDown(BufferedImage source, double factor) {
        int width = Math.max(MIN_DIMENSION, (int) (source.getWidth() * factor));
        int height = Math.max(MIN_DIMENSION, (int) (source.getHeight() * factor));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        scaled.createGraphics().drawImage(source.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        return scaled;
    }

    private byte[] encodeJpeg(BufferedImage image, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG image writer available");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
    }
}
