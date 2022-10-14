package one.whr.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import one.whr.config.RpcServiceConfig;
import one.whr.enums.RpcErrorEnum;
import one.whr.exception.RpcException;
import one.whr.extension.ExtensionLoader;
import one.whr.registry.ServiceProvider;
import one.whr.registry.ServiceRegistry;
import one.whr.remote.transport.server.RpcServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    /**
     * key: rpc service name (interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap;
    private final Set<String> registeredServices;
    private final ServiceRegistry serviceRegistry; // used to register service to ZK

    public ZkServiceProviderImpl() {
        serviceMap = new ConcurrentHashMap<>();
        registeredServices = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension("zk");
    }

    @Override
    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (service == null) {
            throw new RpcException(RpcErrorEnum.SERVICE_NOT_FOUND);
        }
        return service;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.addService(rpcServiceConfig);
            serviceRegistry.registerService(rpcServiceConfig.getRpcServiceName(), new InetSocketAddress(host, RpcServer.PORT));
        } catch (UnknownHostException e) {
            log.error("Exception occurs when getHostAddress", e);
        }
    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {
        String rpcServiceName = rpcServiceConfig.getRpcServiceName();
        if (registeredServices.contains(rpcServiceName)) {
            return;
        }
        registeredServices.add(rpcServiceName);
        serviceMap.put(rpcServiceName, rpcServiceConfig.getService());
        log.info("Add service: {} --- interface: {}", rpcServiceName, rpcServiceConfig.getService().getClass().getInterfaces());
    }
}
