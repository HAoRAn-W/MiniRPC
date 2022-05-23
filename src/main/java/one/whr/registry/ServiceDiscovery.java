package one.whr.registry;

import one.whr.extension.annotation.SPI;
import one.whr.remote.dto.RpcRequest;

import java.net.InetSocketAddress;

@SPI
public interface ServiceDiscovery {
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
