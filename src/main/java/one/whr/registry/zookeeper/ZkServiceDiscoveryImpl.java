package one.whr.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import one.whr.registry.ServiceDiscovery;
import one.whr.remote.dto.RpcRequest;

import java.net.InetSocketAddress;

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        return null;
    }
}
