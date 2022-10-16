package one.whr.proxy;

import lombok.extern.slf4j.Slf4j;
import one.whr.config.RpcServiceConfig;
import one.whr.enums.RpcErrorEnum;
import one.whr.enums.RpcResponseCodeEnum;
import one.whr.exception.RpcException;
import one.whr.remote.dto.RpcRequest;
import one.whr.remote.dto.RpcResponse;
import one.whr.remote.transport.RpcRequestTransport;
import one.whr.remote.transport.client.RpcClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


/**
 * 服务在客户端的动态代理，在初始化Bean之后，生产服务的代理供客户端在本地调用
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";

    // Used for sending requests to the server
    // 客户端将实现这个接口，用于向服务端发送RPC请求
    private final RpcRequestTransport rpcRequestTransport;

    // service config
    private final RpcServiceConfig rpcServiceConfig;

    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, RpcServiceConfig serviceConfig) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.rpcServiceConfig = serviceConfig;
    }


    /**
     * 根据传入的类生产该类的代理类
     * @param clazz 服务端注册的服务类
     * @param <T> 类型标记
     * @return 代理类的实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * 调用时的逻辑
     * @param proxy  the proxy instance that the method was invoked on
     * @param method the {@code Method} instance corresponding to
     *               the interface method invoked on the proxy instance.  The declaring
     *               class of the {@code Method} object will be the interface that
     *               the method was declared in, which may be a superinterface of the
     *               proxy interface that the proxy class inherits the method through.
     * @param args   an array of objects containing the values of the
     *               arguments passed in the method invocation on the proxy instance,
     *               or {@code null} if interface method takes no arguments.
     *               Arguments of primitive types are wrapped in instances of the
     *               appropriate primitive wrapper class, such as
     *               {@code java.lang.Integer} or {@code java.lang.Boolean}.
     * @return RPC调用响应的数据部分
     * @throws ExecutionException 异常
     * @throws InterruptedException 异常
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws ExecutionException, InterruptedException {

        log.info("Method invoked: [{}]", method.getName());  // 日志打印

        RpcRequest rpcRequest = RpcRequest.builder()
                .methodName(method.getName())
                .parameters(args)
                .interfaceName(method.getDeclaringClass().getName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())  // 生成一个UUID
                .group(rpcServiceConfig.getGroup())  // 对应的group
                .version(rpcServiceConfig.getVersion())  // 对应的version
                .build();

        RpcResponse<Object> rpcResponse = null;

        if (rpcRequestTransport instanceof RpcClient) {
            // 异步获取请求结果
            CompletableFuture<RpcResponse<Object>> future =
                    (CompletableFuture<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            rpcResponse = future.get();
        }
        this.check(rpcResponse, rpcRequest);
        return rpcResponse.getData();
    }

    /**
     * 检查响应是否为空，请求和响应的ID是否一致，响应是否成功
     * @param rpcResponse RPC响应对象
     * @param rpcRequest RPC请求对象
     */
    private void check(RpcResponse<Object> rpcResponse, RpcRequest rpcRequest) {
        if (rpcResponse == null) {
            throw new RpcException(RpcErrorEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        if (!rpcRequest.getRequestId().equals(rpcResponse.getRequestId())) {
            throw new RpcException(RpcErrorEnum.REQUEST_NOT_MATCH_RESPONSE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        if (rpcResponse.getCode() == null || !rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
            throw new RpcException(RpcErrorEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
    }

}
