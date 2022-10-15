package one.whr.compress;

import one.whr.annotation.SPI;

/**
 * 压缩解压缩组件的接口
 */
@SPI
public interface Compress {

    byte[] compress(byte[] bytes);

    byte[] decompress(byte[] bytes);
}
