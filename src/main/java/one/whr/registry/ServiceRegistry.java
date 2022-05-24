package one.whr.registry;

import one.whr.annotation.SPI;

import java.net.InetSocketAddress;

@SPI
public interface ServiceRegistry {
    void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress);
}
