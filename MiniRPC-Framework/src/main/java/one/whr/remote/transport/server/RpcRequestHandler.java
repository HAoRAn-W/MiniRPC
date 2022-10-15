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
 * 用于调用请求的服务
 */
@Slf4j
public class RpcRequestHandler {
    private final ServiceProvider serviceProvider;

    public RpcRequestHandler() {
        serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);
    }

    /**
     * 调用服务，返回结果
     * @param rpcRequest RPC请求
     * @return 调用结果
     */
    public Object handle(RpcRequest rpcRequest) {
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * 使用反射调用服务并返回结果
     * @param rpcRequest RPC请求
     * @param service 服务实例
     * @return 调用结果
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
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
