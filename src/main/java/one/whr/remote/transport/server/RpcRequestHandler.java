package one.whr.remote.transport.server;

import lombok.extern.slf4j.Slf4j;
import one.whr.exception.RpcException;
import one.whr.factory.SingletonFactory;
import one.whr.registry.ServiceProvider;
import one.whr.registry.zookeeper.ZkServiceProviderImpl;
import one.whr.remote.dto.RpcRequest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This handler is used to invoke target services and get results
 */
@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    public Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service: [{}] successfully invokes -----> method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
