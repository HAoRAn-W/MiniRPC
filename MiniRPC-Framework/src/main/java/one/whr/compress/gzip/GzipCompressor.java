package one.whr.compress.gzip;

import one.whr.compress.Compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip compressor实现
 */
public class GzipCompressor implements Compress {

    private static final int BUFFER_SIZE = 1024 * 4;

    /**
     * 将bytes压缩
     *
     * @param bytes bytes数组
     * @return 压缩后的byte数组
     */
    @Override
    public byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("input bytes array is null");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(bytes);
            gzip.flush();
            gzip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip compress error", e);
        }
    }

    /**
     * @param bytes 带解压的bytes数组
     * @return 解压后的byte数组
     */
    @Override
    public byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("input bytes array is null");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPInputStream gunzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            // gunzip read from bytes through a ByteArrayInputStream

            byte[] buffer = new byte[BUFFER_SIZE];

            int n;
            while ((n = gunzip.read(buffer)) > -1) {
                // read to a buffer and when full, write the buffer to out
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("gzip decompress error", e);
        }
    }
}
