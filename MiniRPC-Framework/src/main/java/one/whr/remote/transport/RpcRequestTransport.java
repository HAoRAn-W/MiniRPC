package one.whr.remote.transport;

import one.whr.annotation.SPI;
import one.whr.remote.dto.RpcRequest;

import java.util.concurrent.ExecutionException;

/**
 * 客户端发送RPC请求的接口，是调用之后网络传输的开始
 */
@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException;
}
