package one.whr.serialization;

import one.whr.extension.annotation.SPI;

@SPI
public interface Serializer {
    byte[] serialize(Object obj);

    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
